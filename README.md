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
- filtered soft-deleted records directly from queries instead of checking them from the application logics, indexed the "is_deleted" for optimization
- Remove the Cerbos, each service now have to manage it own policies (implement policies as bussiness logics)


-readMessage(serverId) (IS_MEMBER)
-addMessage(serverId, message) (IS_MEMBER)
-addMember(serverId, userId) (IS_OWNER)
-
