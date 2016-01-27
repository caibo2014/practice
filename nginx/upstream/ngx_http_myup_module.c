#include <ngx_core.h>
#include <ngx_http.h>
#include <ngx_config.h>
#include <ngx_http_upstream.h>

// add our handler when myup directives
static char* ngx_http_myup_myup_init(ngx_conf_t *cf, ngx_command_t *cmd, void *conf);
static ngx_int_t ngx_http_myup_handler(ngx_http_request_t *r);
static ngx_int_t myup_create_request(ngx_http_request_t *r);
static void* ngx_http_myup_create_loc_conf(ngx_conf_t *cf);
static char* ngx_http_myup_merge_loc_conf(ngx_conf_t *cf, void *prev, void *conf);
static ngx_int_t myup_process_status_line(ngx_http_request_t *r);
static ngx_int_t myup_process_header(ngx_http_request_t *r);
static void myup_finalize_requset(ngx_http_request_t *r, ngx_int_t rc);

typedef struct {
    ngx_http_upstream_conf_t upstream_conf;
} ngx_http_myup_conf_t;

typedef struct {
    ngx_http_status_t status;
    ngx_str_t server;
} ngx_http_myup_ctx_t;


static ngx_str_t  ngx_http_proxy_hide_headers[] = {

        ngx_string("Date"),
        ngx_string("Server"),
        ngx_string("X-Pad"),
        ngx_string("X-Accel-Expires"),
        ngx_string("X-Accel-Redirect"),
        ngx_string("X-Accel-Limit-Rate"),
        ngx_string("X-Accel-Buffering"),
        ngx_string("X-Accel-Charset"),
        ngx_null_string
};

// commands 
static ngx_command_t ngx_http_myup_commands[] = {
    {
        ngx_string("myup"),
        NGX_HTTP_MAIN_CONF|NGX_HTTP_SRV_CONF|NGX_HTTP_LOC_CONF|NGX_CONF_NOARGS,
        ngx_http_myup_myup_init,
        NGX_HTTP_LOC_CONF_OFFSET,
        0,
        NULL
    },
    ngx_null_command
};

static ngx_http_module_t ngx_http_myup_ctx = {
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    ngx_http_myup_create_loc_conf,
    ngx_http_myup_merge_loc_conf
};

ngx_module_t ngx_http_myup_module = {
    NGX_MODULE_V1,
    &ngx_http_myup_ctx,
    ngx_http_myup_commands,
    NGX_HTTP_MODULE,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL, 
    NGX_MODULE_V1_PADDING	
};

static void* ngx_http_myup_create_loc_conf(ngx_conf_t *cf) {
    ngx_http_myup_conf_t *conf = NULL;
    conf = ngx_pcalloc(cf->pool, sizeof(ngx_http_myup_conf_t));
    // let's set a simple upstream conf
    if (NULL == conf) {
        return conf;   
    }
    conf->upstream_conf.connect_timeout = 60000;
    conf->upstream_conf.send_timeout = 60000;
    conf->upstream_conf.read_timeout = 60000;
    conf->upstream_conf.store_access = 0600;
    conf->upstream_conf.buffering = 0;
    conf->upstream_conf.bufs.num = 8;
    conf->upstream_conf.bufs.size = ngx_pagesize;
    conf->upstream_conf.buffer_size = ngx_pagesize;
    conf->upstream_conf.busy_buffers_size = 2*ngx_pagesize;
    conf->upstream_conf.max_temp_file_size = 1024 * 1024 * 1024;
    conf->upstream_conf.hide_headers = NGX_CONF_UNSET_PTR;
    conf->upstream_conf.pass_headers = NGX_CONF_UNSET_PTR;
    return conf;
}

static ngx_int_t
myup_create_request(ngx_http_request_t *r) {
    static ngx_str_t query_line = ngx_string("GET /s?wd=%V HTTP/1.1 \r\nHost: www.baidu.com\r\nConnection: close\r\n\r\n");
    ngx_int_t query_line_len = query_line.len + r->args.len -2 ;
    ngx_buf_t *b = ngx_create_temp_buf(r->pool, query_line_len);
    b->last = b->pos + query_line_len;

    ngx_snprintf(b->pos, query_line_len, (char*)query_line.data, &r->args);
    r->upstream->request_bufs = ngx_alloc_chain_link(r->pool);

    r->upstream->request_bufs->buf=b;
    r->upstream->request_bufs->next=NULL;
    r->upstream->request_sent=0;
    r->upstream->header_sent=0;
    r->header_hash=1;

    return NGX_OK;
}

static char* ngx_http_myup_merge_loc_conf(ngx_conf_t *cf, void *prev, void *conf) {
	ngx_http_myup_conf_t *parent = (ngx_http_myup_conf_t *)prev;
	ngx_http_myup_conf_t *child = (ngx_http_myup_conf_t *)conf;
	
	ngx_hash_init_t hash;
	hash.max_size = 100;
	hash.bucket_size = 1024;
	hash.name = "proxy_header_hash";
	if (ngx_http_upstream_hide_headers_hash(cf, &child->upstream_conf, &parent->upstream_conf,
							ngx_http_proxy_hide_headers, &hash) != NGX_OK){
		return NGX_CONF_ERROR;
	}
	return NGX_CONF_OK;
}

static ngx_int_t myup_process_status_line(ngx_http_request_t *r) {
		size_t len;
		ngx_int_t rc;
		ngx_http_upstream_t *u;
		
		ngx_http_myup_ctx_t *ctx = ngx_http_get_module_ctx(r, ngx_http_myup_module);
		if(ctx == NULL) {
			return NGX_ERROR;		
		}
		u = r->upstream;
		rc = ngx_http_parse_status_line(r, &u->buffer, &ctx->status);
		if (rc == NGX_AGAIN) {
			return rc;
		}
		if (rc == NGX_ERROR) {
			ngx_log_error(NGX_LOG_ERR, r->connection->log, 0, "rc == NGX_ERROR");
			r->http_version= NGX_HTTP_VERSION_9;
			u->state->status=NGX_HTTP_OK;
			return NGX_OK;
		}
		if (u->state) {
			u->state->status = ctx->status.code;
		}
		u->headers_in.status_n = ctx->status.code;
		len = ctx->status.end - ctx->status.start;
		u->headers_in.status_line.len = len;

		u->headers_in.status_line.data = ngx_pnalloc(r->pool,len);
		if (u->headers_in.status_line.data == NULL) {
			return NGX_ERROR;
		}

		ngx_memcpy(u->headers_in.status_line.data, ctx->status.start, len);
		u->process_header = myup_process_header;
		return myup_process_header(r);
}


static ngx_int_t myup_process_header(ngx_http_request_t *r) {
    ngx_int_t rc;
    ngx_table_elt_t *h;
    ngx_http_upstream_header_t *hh;
    ngx_http_upstream_main_conf_t *umcf;

    umcf = ngx_http_get_module_main_conf(r, ngx_http_upstream_module);
    for(;;) {
        rc = ngx_http_parse_header_line(r, &r->upstream->buffer,1);
        if(rc == NGX_OK) {

            h = ngx_list_push(&r->upstream->headers_in.headers);
            if  (NULL ==  h){
                return NGX_ERROR;
            }
            h->hash = r->header_hash;

            h->key.len=r->header_name_end - r->header_name_start;
            h->value.len = r->header_end - r->header_start;
            h->key.data=ngx_pnalloc(r->pool,h->key.len+1+h->value.len+1+h->key.len);
            if(h->key.data == NULL)  {
                return  NGX_ERROR;
            }
            h->value.data =  h->key.data+h->key.len+1;
            h->lowcase_key = h->key.data + h->key.len+1+h->value.len+1;
            ngx_memcpy(h->key.data, r->header_name_start, h->key.len);
            h->key.data[h->key.len] = '\0';
            ngx_memcpy(h->value.data, r->header_start, h->value.len);
            h->value.data[h->value.len] = '\0';

            if(h->key.len > r->lowcase_index) {
                ngx_memcpy(h->lowcase_key, r->lowcase_header, h->key.len);
            } else {
                ngx_strlow(h->lowcase_key, h->key.data, h->key.len);
            }

            hh = ngx_hash_find(&umcf->headers_in_hash, h->hash,
                               h->lowcase_key, h->key.len);
            if (hh && hh->handler(r, h, hh->offset)!= NGX_OK) {
                return  NGX_ERROR;
            }
            continue;
        }
        if (rc == NGX_HTTP_PARSE_HEADER_DONE) {
            if(r->upstream->headers_in.server == NULL) {
                h = ngx_list_push(&r->upstream->headers_in.headers);
                if(h == NULL){
                    return NGX_ERROR;
                }
                h->hash=ngx_hash(ngx_hash(ngx_hash(ngx_hash(ngx_hash('s','e'),'r'),'v'),'e'),'r');
                ngx_str_set(&h->key, "Server");
                ngx_str_null(&h->value);
                h->lowcase_key =(u_char *)"server";
            }
            if(r->upstream->headers_in.date == NULL) {
                h =ngx_list_push(&r->upstream->headers_in.headers);
                if(h ==NULL){
                    return NGX_ERROR;
                }
                h->hash= ngx_hash(ngx_hash(ngx_hash('d', 'a'),'t'),'e');
                ngx_str_set(&h->key,"Date");
                ngx_str_null(&h->value);
                h->lowcase_key = (u_char*)"date";
            }
            return NGX_OK;
        }

    if(rc==NGX_AGAIN) {
        return rc;
    }
    ngx_log_error(NGX_LOG_ERR, r->connection->log,0,"error");
    return NGX_HTTP_UPSTREAM_INVALID_HEADER;
    }
}

static void myup_finalize_requset(ngx_http_request_t *r, ngx_int_t rc){
    ngx_log_error(NGX_LOG_INFO,r->connection->log,0,"finalize");
}


static ngx_int_t ngx_http_myup_handler(ngx_http_request_t *r) {
    ngx_log_error(NGX_LOG_ERR, r->connection->log, 0, "myup handler has been called!");

    // create myup context
    ngx_http_myup_ctx_t* myctx = ngx_http_get_module_ctx(r, ngx_http_myup_module);
    if (NULL == myctx) {
        myctx = ngx_pcalloc(r->pool, sizeof(ngx_http_myup_ctx_t));
        if (NULL == myctx) {
            return NGX_ERROR;
        }
    }
    // ref myup context and request
    ngx_http_set_ctx(r, myctx, ngx_http_myup_module);
    // create a upstream
    if (ngx_http_upstream_create(r) != NGX_OK) {
        ngx_log_error(NGX_LOG_ERR, r->connection->log, 0, "ngx_http_upstream_create error");
        return NGX_ERROR;
    }

    // get my config struct
    ngx_http_myup_conf_t *mycf = (ngx_http_myup_conf_t *)ngx_http_get_module_loc_conf(r, ngx_http_myup_module);

    // let's make a simple upstream conf
    ngx_http_upstream_t *u = r->upstream;
    u->conf = &mycf->upstream_conf;
    u->buffering = mycf->upstream_conf.buffering;

    u->resolved = (ngx_http_upstream_resolved_t*) ngx_pcalloc(r->pool, sizeof(ngx_http_upstream_resolved_t));
    if(u->resolved == NULL) {
        return NGX_ERROR;
    }

    static struct sockaddr_in backendSockAddr;
    struct hostent *pHost = gethostbyname((char*)"www.baidu.com");
    if(pHost == NULL) {
        return NGX_ERROR;
    }

    backendSockAddr.sin_family = AF_INET;
    backendSockAddr.sin_port = htons((in_port_t)80);
    char *pDmsIp = inet_ntoa(*(struct in_addr*)(pHost->h_addr_list[0]));
    backendSockAddr.sin_addr.s_addr = inet_addr(pDmsIp);
    myctx->server.data = (u_char*)pDmsIp;
    myctx->server.len = strlen(pDmsIp);

    u->resolved->sockaddr = (struct sockaddr*)&backendSockAddr;
    u->resolved->socklen=sizeof(struct sockaddr_in);
    u->resolved->naddrs=1;
    // some bug in nginx 1.99? we must set the port....funny...
    u->resolved->port = (in_port_t)80;

    u->create_request = myup_create_request;
    u->process_header = myup_process_status_line;
    u->finalize_request = myup_finalize_requset;

    r->main->count++;
    ngx_http_upstream_init(r);

    return NGX_DONE;
}

static char* ngx_http_myup_myup_init(ngx_conf_t *cf, ngx_command_t *cmd, void *conf) {
    ngx_http_core_loc_conf_t *clcf;
    clcf = ngx_http_conf_get_module_loc_conf(cf, ngx_http_core_module);
    if (NULL == clcf) {
        return NGX_CONF_ERROR;
    }	
    clcf->handler = ngx_http_myup_handler;
    return NGX_CONF_OK;
}
