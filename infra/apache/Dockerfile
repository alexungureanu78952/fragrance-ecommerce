FROM httpd:2.4-alpine

# Enable the modules the demo needs and load the site configuration.
RUN sed -i \
      -e 's/^#\(LoadModule headers_module\)/\1/' \
      -e 's/^#\(LoadModule proxy_module\)/\1/' \
      -e 's/^#\(LoadModule proxy_http_module\)/\1/' \
      conf/httpd.conf \
 && echo 'Include conf/extra/fragrance.conf' >> conf/httpd.conf

COPY fragrance.conf conf/extra/fragrance.conf
