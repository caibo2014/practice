/*
 * Copyright (C) Bo Cai
 * Copyright (C) Fugle, Inc.
 */


#include <ngx_config.h>
#include <ngx_core.h>
#include <ngx_http.h>
#include <string.h>

typedef struct {
  ngx_str_t name;
} ngx_http_valar_vars_t;

static ngx_int_t  ngx_http_valar_init(ngx_conf_t *cf);
static void* ngx_http_valar_create_loc_conf(ngx_conf_t *cf);

static ngx_command_t ngx_http_valar_command[] = {
    { ngx_string("valar"),
      NGX_HTTP_MAIN_CONF|NGX_HTTP_SRV_CONF|NGX_HTTP_LOC_CONF|NGX_CONF_TAKE1,
      ngx_conf_set_str_slot,
      NGX_HTTP_LOC_CONF_OFFSET,
      offsetof(ngx_http_valar_vars_t, name),
      NULL },
      
      ngx_null_command
};


static ngx_http_module_t ngx_http_valar_module_ctx = {
        NULL, 
	ngx_http_valar_init,
	NULL,
	NULL,
	NULL,
	NULL,
	ngx_http_valar_create_loc_conf,
	NULL
};


ngx_module_t ngx_http_valar_module = {
        NGX_MODULE_V1,
	&ngx_http_valar_module_ctx, 
	ngx_http_valar_command,      
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

static void* ngx_http_valar_create_loc_conf(ngx_conf_t *cf){
    ngx_http_valar_vars_t *myconf = NULL;
    myconf = (ngx_http_valar_vars_t *)ngx_pcalloc(cf->pool, sizeof(ngx_http_valar_vars_t));
    if (NULL == myconf) {
      return NULL;
    }
    return myconf;
}

ngx_int_t ngx_http_valar_handler(ngx_http_request_t *r) {
	
	ngx_log_error(NGX_LOG_ERR, r->connection->log, 0, "valar module have been called!");
	ngx_http_valar_vars_t *mycf;
	char out_str[1024] = "the valar config name is: ";
        mycf = ngx_http_get_module_loc_conf(r, ngx_http_valar_module);
        if (NULL == mycf) {
           return NGX_ERROR;
	}
        // copy config string in my char array
        ngx_str_t myname = mycf->name;
        strncat(out_str, (const char*)myname.data, myname.len);
        
	if ((r->method & (NGX_HTTP_GET | NGX_HTTP_POST)) == 0) {
		return NGX_HTTP_NOT_ALLOWED;
	}
	ngx_int_t rc = ngx_http_discard_request_body(r);
	if (rc != NGX_OK) {
		return rc;
	}
        
	ngx_str_t type = ngx_string("text/plain");
	ngx_str_t response = ngx_null_string;
        response.data = (u_char*)out_str;
	response.len = strlen(out_str);

	r->headers_out.status = NGX_HTTP_OK;
	r->headers_out.content_type = type;
	r->headers_out.content_length_n = response.len;

	rc = ngx_http_send_header(r);
	if (rc == NGX_ERROR || rc > NGX_OK || r->header_only) {
		return rc;
	}

	ngx_buf_t *b;
	b = ngx_create_temp_buf(r->pool, response.len);
	if (b == NULL) {
		return NGX_HTTP_INTERNAL_SERVER_ERROR;
	}
	ngx_memcpy(b->pos, response.data, response.len);
	b->last = b->pos + response.len;
	// this is the last buffer
	b->last_buf = 1;

	ngx_chain_t out;
	out.buf = b;
	out.next = NULL;	
	return ngx_http_output_filter( r, &out);
}


static ngx_int_t ngx_http_valar_init(ngx_conf_t *cf) {
        ngx_http_core_main_conf_t *cmcf;
        ngx_http_handler_pt *h;
        cmcf = ngx_http_conf_get_module_main_conf(cf, ngx_http_core_module);
        if (NULL == cmcf) {
	    return NGX_ERROR;
	}
        h = ngx_array_push(&cmcf->phases[NGX_HTTP_CONTENT_PHASE].handlers);
	if (NULL == h) {
	   return NGX_ERROR;
	}
        *h = ngx_http_valar_handler;
	return NGX_OK;       
}




