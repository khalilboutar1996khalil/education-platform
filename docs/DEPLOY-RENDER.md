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

## Fichiers déposés — Cloudflare R2

Sur Render gratuit, le disque est effacé à chaque redéploiement : les PDF et devoirs déposés y
disparaîtraient. Dès que `STORAGE_S3_BUCKET` est renseigné, l'API range les fichiers dans un bucket
compatible S3 (Cloudflare R2, gratuit jusqu'à 10 Go) ; sinon elle écrit sur le disque, comme en local.

1. dash.cloudflare.com → **R2 Object Storage** → **Create bucket** : `eduflow-uploads`, emplacement
   automatique. Le bucket reste **privé** : les téléchargements passent par l'API, qui vérifie les droits.
2. **R2 → Manage API tokens → Create API token** : permission **Object Read & Write**, limitée au
   bucket `eduflow-uploads`. Noter l'**Access Key ID** et le **Secret Access Key** (affiché une seule fois).
3. L'**endpoint** est `https://<account-id>.r2.cloudflarestorage.com` (affiché sur la page du bucket).
4. Render → `eduflow-api` → **Environment** :

| Variable                | Valeur                                              |
|-------------------------|-----------------------------------------------------|
| `STORAGE_S3_ENDPOINT`   | `https://<account-id>.r2.cloudflarestorage.com`     |
| `STORAGE_S3_BUCKET`     | `eduflow-uploads`                                   |
| `STORAGE_S3_ACCESS_KEY` | l'Access Key ID                                     |
| `STORAGE_S3_SECRET_KEY` | le Secret Access Key                                |

Les fichiers déposés avant ce réglage étaient sur le disque de Render : ils sont déjà perdus et
doivent être redéposés.
