# Reset Vault

Wipes all stored secrets and starts over. `data/` is owned by the container user, so remove it through Docker.

```bash
cd infrastructure/components/vault
docker compose -p vault_service_chatter -f docker-compose.yml down
docker run --rm --entrypoint sh -v "$PWD/data:/d" hashicorp/vault:1.17.6 -c 'rm -rf /d/* /d/.[!.]*'
rm -f .vault-init
```

Then follow [bootstrap](bootstrap.md) and [configure secrets](configure-secrets.md). If you keep `VAULT_UNSEAL_KEY` or `VAULT_TOKEN` in `.env`, update or remove them: the new Vault has different values.
