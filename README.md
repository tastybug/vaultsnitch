# VaultPal

java17 or higher.

`mvn clean package`, then `java -jar target/vaultpal-1.0-SNAPSHOT.jar`.


```shell
docker run -d --name vault -p 8200:8200 -e 'VAULT_DEV_ROOT_TOKEN_ID=myroot' hashicorp/vault:1.19
docker exec  -ti vault /bin/ash
```
