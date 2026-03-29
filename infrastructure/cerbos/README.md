## Pulling via Docker
```powershell
docker compose -p cerbos_chatter -f docker-compose.yml up -d
```

## Development credentials
```
username: admin
password: GHGUGhvTzHlXzdGI
bcrypt hash: $2a$12$E/yoK8YjkWNy3mlDHkeLSe17q0e9IB8DFrzuPKqDHKWfmUD79aq1e
base64-encoded bcrypt hash: JDJhJDEyJEUveW9LOFlqa1dOeTNtbERIa2VMU2UxN3EwZTlJQjhERnJ6dVBLcURIS1dmbVVENzlhcTFl
```

## Configuration file

The Cerbos runtime configuration is here:

```
./docker/config/config.yaml
```

Full configuration docs: https://docs.cerbos.dev/cerbos/0.44.0/configuration/

## Policies

Learn how policies work and how to author them:

https://docs.cerbos.dev/cerbos/latest/policies/

## Quick troubleshooting

- Follow container logs to see startup errors:

```bash
docker compose -p cerbos_service_chatter -f docker-compose.yml logs -f
```

- If Cerbos fails to start or returns configuration errors:
  - Validate `./docker/config/config.yaml` syntax (YAML indentation and paths).
  - Check file permissions for any referenced policy directories or cert files.
  - Confirm ports are not already in use by other services.

## References

- Cerbos official docs: https://docs.cerbos.dev/
