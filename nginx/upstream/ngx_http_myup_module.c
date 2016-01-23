#include <ngx_core.h>
#include <ngx_http.h>
#include <ngx_config.h>

// add our handler when myup directives
static char* ngx_http_myup_myup_init(ngx_conf_t *cf, ngx_command_t *cmd, void *conf);
static ngx_int_t ngx_http_myup_handler(ngx_http_request_t *r);
static void* ngx_http_myup_create_loc_conf(ngx_conf_t *cf);
static char* ngx_http_myup_merge_loc_conf(ngx_conf_t *cf, void *prev, void *conf);
static ngx_int_t myup_process_status_line(ngx_http_request_t *r);
static ngx_int_t myup_process_header(ngx_http_request_t *r);

typedef struct {
    ngx_http_upstream_conf_t upstream_conf;
} ngx_http_myup_conf_t;

typedef struct {
    ngx_http_status_t status;
} ngx_http_myup_ctx_t;

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
    ngx_http_myup_config_t *conf = NULL;
    conf = ngx_pcalloc(cf->pool, sizeof(ngx_http_myup_config_t));
    // let's set a simple upstream conf
    if (NULL == conf) {
        return conf;   
    }
    conf->upstream_conf.connection_timeout = 60000;
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
myup_upstream_create_request(ngx_http_requeset_t *r) {
    static ngx_str_t query_line = ngx_string("GET /s?wd=%V HTTP/1.1 \r\nHost: www.baidu.com\r\nConnection: close\r\n\r\n");
    ngx_int_t query_line_len = query_line.len + r->args.len -2 ;
    ngx_buf_t *b = ngx_create_tmp_buf(r->pool, query_line_len);
    b->last = b->pos + query_line_len;

    ngx_snprintf(b->pos, query_line_len, (char*)query_line.data, &r->args);
}

static char* ngx_http_myup_merge_loc_conf(ngx_conf_t *cf, void *prev, void *conf) {
	ngx_http_myup_conf_t *parent = (ngx_http_mytest_conf_t *)prev;
	ngx_http_myup_conf_t *child = (ngx_http_mytest_conf_t *)conf;
	
	ngx_hash_init_t hash;
	hash.max_size = 100;
	hash.bucket_size = 1024;
	hash.name = "proxy_header_hash";
	if (ngx_http_upstream_hide_headers_hash(cf, &child->upstream, &parent->upstream,
							ngx_http_proxy_hide_headers, &hash) != NGX_OK){
		return NGX_CONF_ERROR;
	}
	return NGX_CONF_OK;
}

static ngx_int_t myup_process_status_line(ngx_http_request_t *r) {
		size_t len;
		ngx_int_t rc;
		ngx_http_upstream_t *u;
		
		ngx_http_myup_ctx_t *ctx = ngx_http_get_module_ctx(r, ngx_http_mytest_module);
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
		u->headers_in.status_n = ctx->status.start;
		len = ctx->status.end - ctx->status.start;
		u->header_in.status_line.len = len;

		u->headers_in.status_line.data = ngx_pnalloc(r->poll,len);
		if (u->headers_in.status_line.data == NULL) {
			return NGX_ERROR;
		}

		ngx_memcpy(u->headers_in.status_line.data, ctx->status.start, len);
		u->process_header = myup_upstream_process_header;
		return myup_upstream_process_header(r);
}

static ngx_int_t ngx_http_myup_handler(ngx_http_request_t *r) {
    ngx_log_error(NGX_LOG_ERR, r->connection->log, 0, "myup handler has been called!");
    /*
    // create myup context
    ngx_http_myup_ctx *myctx = ngx_http_get_module_ctx(r, ngx_http_myup_module);
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
    ngx_http_myup_config_t *mycf = (ngx_http_myup_config_t *)ngx_http_get_module_loc_conf(r, ngx_http_myup_module);
    */

    // let's make a simple upstream conf
    //
    return NGX_ERROR;
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
