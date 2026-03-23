## Pulling via Docker
```powershell
docker compose -p api-gateway_chatter -f enviroment/docker-compose.yml up -d
```

**Improvement notes:**
- The current rate limiting uses the built-in rate limiting of Spring Cloud Gateway and it is less of features than Bucket4j
- Spring Cloud Gateway's rate limiting supports auto-loading from property files but Bucket4j doesn't, so if we planted to move to Bucket4j, we would need to write custom configurations by ourselves
- All of the current rate limit configurations are hard coded inside the "config/RateLimiterConfig", and they should be moved to property files for more flexibility
- Consider removing the Vault key rotation logic, implement that feature for a Redis storage which is used only for storing buckets may be overheaded