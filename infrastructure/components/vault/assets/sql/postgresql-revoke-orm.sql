-- Hand every object owned by this role to the account Vault connects as
REASSIGN OWNED BY "{{name}}" TO CURRENT_USER;

-- Drop any remaining privileges
DROP OWNED BY "{{name}}";

-- Drop the role safely
DROP ROLE IF EXISTS "{{name}}";