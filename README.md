#Postgres command 

`docker ru --name gp-postgres -e POSTGRES_PASSWORD=123 -e POSTGRRES_USER=admin -e POSTGRES_HOST_AUTH_METHOD=password -d -p 5432:5432 postgres:14.2-alpine` 