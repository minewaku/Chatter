## HOW TO BUILD?


# Build the entire project (include all child services)
```bash
.\mvnw clean install
```

# Build the specific service
```bash
.\mvnw clean package -pl services/service-registry
```

## DEV NOTE
- remember to modify all .properties to let them read value from .env then default vaule