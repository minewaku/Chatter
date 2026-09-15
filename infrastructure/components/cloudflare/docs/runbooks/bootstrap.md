# Cloudflare Tunnel Runbook

1. **Create/Retrieve Tunnel Token**  
   - In Cloudflare Zero Trust: `Networks → Tunnels → Create tunnel`.  
   - Copy the generated token.

2. **Set the Token**  
   ```powershell
   $env:TUNNEL_TOKEN="<your token>"
   ```
   or add it to an `.env` file consumed by Docker Compose.

3. **Start the Tunnel**  
   ```powershell
   docker compose -p cloudflared_chatter -f docker-compose.yml up -d
   ```

4. **Verify**  
   - `docker logs cloudflared-chatter` should show “Connection established”.  
   - Visit the Zero Trust dashboard to confirm healthy status.

5. **Stop the Tunnel**  
   ```powershell
   docker compose -p cloudflared_chatter -f docker-compose.yml down
   ```

6. **Expose Additional Services**  
   - Edit `docker-compose.yml` to include new dependencies.  
   - Update the Cloudflare tunnel configuration (public hostname → local service) via the dashboard.
