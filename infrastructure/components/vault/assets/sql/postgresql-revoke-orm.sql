-- Hand every object owned by this role to the fixed admin account ('vault')
REASSIGN OWNED BY "{{name}}" TO vault;

-- Drop any remaining privileges
DROP OWNED BY "{{name}}";

-- Drop the role safely
DROP ROLE IF EXISTS "{{name}}";