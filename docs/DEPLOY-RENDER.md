# Déployer EduFlow gratuitement : Render + Neon + Vercel

```
Navigateur ──▶ Vercel (Angular) ──▶ Render (API Spring Boot) ──▶ Neon (PostgreSQL)
                                           └──▶ Brevo (e-mails, API HTTPS)
```

Tout est gratuit. Limites à connaître :

- **Render gratuit** : l'API s'endort après 15 min sans visite ; la première requête prend ~30–60 s.
- **Render gratuit bloque le SMTP** (ports 25, 465, 587) : les e-mails passent par l'API HTTPS de Brevo.
- **Render gratuit n'a pas de disque permanent** : les fichiers déposés (`var/uploads`) sont perdus
  à chaque redémarrage ou redéploiement. Voir « Fichiers déposés » plus bas.

---

## 1. Base de données — Neon

1. Projet Neon, région **Frankfurt**, *Connection pooling* désactivé.
2. Dans **Connect**, la chaîne ressemble à
   `postgresql://UTILISATEUR:MOTDEPASSE@HOTE/BASE?sslmode=require&channel_binding=require`.
3. Découpe-la en 3 variables :

| Variable      | Valeur                                              |
|---------------|-----------------------------------------------------|
| `DB_URL`      | `jdbc:postgresql://HOTE/BASE?sslmode=require`       |
| `DB_USERNAME` | `UTILISATEUR` (ex. `neondb_owner`)                  |
| `DB_PASSWORD` | `MOTDEPASSE`                                        |

Flyway crée toutes les tables au premier démarrage. Ne mets jamais ces valeurs dans le code ni sur GitHub.

## 2. E-mails — Brevo

1. Compte gratuit sur brevo.com (300 e-mails/jour).
2. **Senders** : ajoute et vérifie l'adresse d'envoi → ce sera `APP_MAIL_FROM`.
3. **SMTP & API → API Keys** : crée une clé → ce sera `BREVO_API_KEY`.

Dès que `BREVO_API_KEY` est définie, l'application envoie par Brevo (prioritaire sur le SMTP).
Sans aucune configuration, les e-mails sont seulement écrits dans les logs.

## 3. API — Render

1. Render → **New → Blueprint** → ce dépôt. Le fichier `render.yaml` configure tout
   (Docker, plan gratuit, Frankfurt, health check).
2. Render demande les valeurs secrètes :

| Variable               | Valeur                                                |
|------------------------|-------------------------------------------------------|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | voir étape 1                          |
| `CORS_ALLOWED_ORIGINS` | l'adresse Vercel, ex. `https://eduflow.vercel.app`    |
| `FRONTEND_URL`         | la même adresse Vercel (liens dans les e-mails)       |
| `BREVO_API_KEY`        | voir étape 2                                          |
| `APP_MAIL_FROM`        | l'expéditeur vérifié dans Brevo                       |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | le premier administrateur (créé au 1er démarrage) |

`JWT_SECRET` est généré automatiquement par Render.

Tant que Vercel n'est pas déployé, mets `http://localhost:4200` dans `CORS_ALLOWED_ORIGINS`
et `FRONTEND_URL`, puis remplace-les par l'adresse Vercel.

3. Vérifie : `https://<ton-service>.onrender.com/actuator/health` doit répondre `{"status":"UP"}`.

## 4. Site — Vercel

Dans le projet Angular (`education-platform-front`) :

- l'URL de l'API de production pointe vers `https://<ton-service>.onrender.com` ;
- un `vercel.json` renvoie toutes les routes vers `index.html` :

```json
{ "rewrites": [{ "source": "/(.*)", "destination": "/index.html" }] }
```

Puis Vercel → **Add New → Project** → importer le dépôt front.

## 5. Tester en local avec Neon

Dans le fichier `.env` (jamais commité) :

```properties
DB_URL=jdbc:postgresql://HOTE/BASE?sslmode=require
DB_USERNAME=neondb_owner
DB_PASSWORD=...
BREVO_API_KEY=...
APP_MAIL_FROM=...
FRONTEND_URL=http://localhost:4200
CORS_ALLOWED_ORIGINS=http://localhost:4200
```

Puis `./gradlew bootRun`.

## Fichiers déposés : Cloudflare R2

Sur Render gratuit, le disque est effacé à chaque redémarrage : les fichiers déposés (rendus de TP,
ressources, PDF) seraient perdus. L'API sait donc écrire dans un stockage objet compatible S3.
Cloudflare R2 est gratuit jusqu'à 10 Go.

1. Cloudflare → **R2** → **Create bucket** : nom `eduflow` (laisser privé, pas d'accès public).
2. R2 → **Manage R2 API Tokens** → **Create API token** : permission **Object Read & Write**,
   limitée au bucket `eduflow`. Notez l'**Access Key ID**, le **Secret Access Key** et l'adresse
   **S3 API** (`https://<account-id>.r2.cloudflarestorage.com`).
3. Render → service `eduflow-api` → **Environment** :

| Variable | Valeur |
|---|---|
| `STORAGE_TYPE` | `s3` |
| `S3_ENDPOINT` | `https://<account-id>.r2.cloudflarestorage.com` |
| `S3_REGION` | `auto` |
| `S3_BUCKET` | `eduflow` |
| `S3_ACCESS_KEY` | l'Access Key ID |
| `S3_SECRET_KEY` | le Secret Access Key |

4. Enregistrez : Render redémarre l'API. Déposez un PDF dans Ressources, redéployez, puis
   téléchargez-le : il doit toujours être là.

### Variante gratuite sans carte bancaire : Supabase Storage

1. supabase.com → **New project** (région Frankfurt) → **Storage** → **New bucket** `eduflow`, privé.
2. **Storage → S3 Configuration** : copier l'**Endpoint**
   (`https://<projet>.storage.supabase.co/storage/v1/s3`), la **Region** (ex. `eu-central-1`),
   puis **New access key** (Access key ID + Secret access key).
3. Mêmes variables Render que ci-dessus, avec `S3_REGION` = la région Supabase.

Limites du plan gratuit : 1 Go au total, 50 Mo par fichier, et le projet se met en pause après
7 jours sans activité (les fichiers restent ; « Restore » dans Supabase).

Sans `STORAGE_TYPE` (ou avec `local`), l'API garde le disque du serveur, comme en développement.
Les fichiers déposés **avant** le passage à R2 ne sont pas déplacés : ils étaient déjà perdus au
dernier redémarrage, il faut les déposer à nouveau.

## Garder l'API réveillée

Le workflow `.github/workflows/keep-awake.yml` appelle `/actuator/health/liveness` toutes les
10 minutes, pour que l'API ne s'endorme pas (sinon la première visite attend 30 à 60 s, par
exemple un élève qui scanne un QR code du livre). La sonde *liveness* n'interroge pas la base,
donc Neon peut continuer à se mettre en veille.

- L'adresse appelée est la variable de dépôt `API_URL` (GitHub → Settings → Secrets and variables →
  Actions → **Variables**), par défaut `https://eduflow-api-iqq9.onrender.com`.
- Une API toujours éveillée consomme environ 744 h par mois ; le plan gratuit de Render en donne
  750 par compte. Si vous avez d'autres services gratuits sur Render, ils partagent ce total.
- GitHub peut retarder les tâches planifiées de quelques minutes, et les suspend après 60 jours
  sans activité sur le dépôt.
