#include <ngx_core.h>
#include <ngx_http.h>
#include <ngx_config.h>

// add our handler when myup directives
static char* ngx_http_myup_myup_init(ngx_conf_t *cf, ngx_command_t *cmd, void *conf);
static ngx_int_t ngx_http_myup_handler(ngx_http_request_t *r);

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
	NULL,
	NULL
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


static ngx_int_t ngx_http_myup_handler(ngx_http_request_t *r) {
	ngx_log_error(NGX_LOG_ERR, r->connection->log, 0, "myup handler has been called!");
	
	return NGX_HTTP_NOT_ALLOWED;
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
