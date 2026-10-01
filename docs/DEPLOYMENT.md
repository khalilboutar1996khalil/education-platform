# Déployer EduFlow sur un VPS Hostinger

Ce guide met en ligne **toute la plateforme** (Angular + Spring Boot + PostgreSQL) sur un seul serveur,
avec Docker Compose. Compte environ **1 heure** la première fois.

```
Internet ──▶ web (Caddy : Angular + HTTPS) ──▶ api (Spring Boot) ──▶ postgres
             ports 80/443 ouverts              privé                privé
```

Seul Caddy est visible depuis Internet. L'API et la base ne sont joignables qu'à l'intérieur du serveur.

---

## 1. Commander le VPS

1. Sur Hostinger, choisis **VPS → KVM 1** (1 vCPU, 4 Go RAM, 50 Go disque) — largement suffisant.
   ⚠️ Pas l'« Hébergement Web » : il ne fait tourner ni Java ni Docker.
2. Système d'exploitation : **Ubuntu 24.04 with Docker** (dans « Applications » / « OS with Panel »).
   Si tu ne le trouves pas, prends **Ubuntu 24.04** simple : on installera Docker à l'étape 3.
3. Choisis un **mot de passe root fort** (ou mieux, ajoute ta clé SSH).
4. Note l'**adresse IP** du VPS (panneau Hostinger → VPS → Vue d'ensemble). Dans ce guide : `203.0.113.10`.

## 2. Se connecter au serveur

Depuis le Terminal de ton Mac :

```bash
ssh root@203.0.113.10
```

Mettre le système à jour :

```bash
apt update && apt upgrade -y
```

## 3. Installer Docker (si l'image ne l'a pas déjà)

```bash
docker --version || curl -fsSL https://get.docker.com | sh
docker compose version
```

## 4. Pare-feu

N'ouvre que SSH, HTTP et HTTPS :

```bash
ufw allow OpenSSH
ufw allow 80
ufw allow 443
ufw --force enable
```

> Hostinger a aussi un pare-feu dans son panneau (VPS → Sécurité → Pare-feu) : s'il est actif,
> autorise-y les mêmes ports 22, 80 et 443.

## 5. Récupérer le code

Les deux projets doivent être **côte à côte** dans le même dossier :

```bash
mkdir -p /opt/eduflow && cd /opt/eduflow
git clone https://github.com/khalilboutar1996khalil/education-platform.git
git clone https://github.com/khalilboutar1996khalil/education-platform-front.git
```

> Si tes dépôts GitHub sont **privés**, `git clone` demandera un identifiant : utilise ton nom
> d'utilisateur GitHub et un **Personal Access Token** (GitHub → Settings → Developer settings →
> Personal access tokens) à la place du mot de passe.

## 6. Configurer les secrets

```bash
cd /opt/eduflow/education-platform
cp .env.production.example .env
openssl rand -base64 32    # lance-le 2 fois : une valeur pour DB_PASSWORD, une pour JWT_SECRET
nano .env
```

À remplir au minimum :

| Variable | Valeur |
|---|---|
| `SITE_ADDRESS` | `:80` (pas encore de domaine) |
| `PUBLIC_URL` | `http://203.0.113.10` (ton IP) |
| `DB_PASSWORD` | une valeur générée par `openssl` |
| `JWT_SECRET` | une **autre** valeur générée par `openssl` |
| `ADMIN_EMAIL` | ton e-mail de connexion admin |
| `ADMIN_PASSWORD` | au moins 10 caractères |
| `ADMIN_FULL_NAME` | ton nom |

Enregistre avec `Ctrl+O`, `Entrée`, puis quitte avec `Ctrl+X`. Protège le fichier :

```bash
chmod 600 .env
```

## 7. Lancer la plateforme

```bash
docker compose -f compose.prod.yaml up -d --build
```

Le premier lancement compile Angular et Spring Boot : **5 à 10 minutes**. Ensuite :

```bash
docker compose -f compose.prod.yaml ps                # les 3 services doivent être "running" / "healthy"
docker compose -f compose.prod.yaml logs -f api       # Ctrl+C pour quitter les logs
```

Dans les logs de l'API, tu dois voir `First administrator … created` puis `Started EducationPlatformApplication`.

## 8. Vérifier

| Test | Résultat attendu |
|---|---|
| `http://203.0.113.10/actuator/health` | `{"status":"UP",…}` |
| `http://203.0.113.10` | la page de connexion Angular |
| Connexion avec `ADMIN_EMAIL` / `ADMIN_PASSWORD` | le tableau de bord admin |

Une fois connecté : **retire `ADMIN_PASSWORD` du fichier `.env`** (il ne sert plus) et change ton mot
de passe depuis la page Paramètres.

---

## Mettre à jour après une modification du code

```bash
cd /opt/eduflow/education-platform-front && git pull
cd /opt/eduflow/education-platform && git pull
docker compose -f compose.prod.yaml up -d --build
```

Les migrations Flyway s'appliquent toutes seules au démarrage. Les données et les fichiers sont conservés.

## Sauvegardes

Sauvegarde manuelle :

```bash
cd /opt/eduflow/education-platform && ./deploy/backup.sh
```

Sauvegarde automatique chaque nuit à 3 h (garde 14 jours) :

```bash
crontab -e
# ajoute cette ligne :
0 3 * * * /opt/eduflow/education-platform/deploy/backup.sh >> /var/log/eduflow-backup.log 2>&1
```

Copie régulièrement `/opt/eduflow/backups` sur ton Mac — une sauvegarde sur le même disque ne
protège pas d'une panne de ce disque :

```bash
scp -r root@203.0.113.10:/opt/eduflow/backups ~/eduflow-backups
```

Restaurer la base depuis une sauvegarde :

```bash
gunzip -c /opt/eduflow/backups/db_AAAA-MM-JJ_HHMM.sql.gz \
  | docker compose -f compose.prod.yaml exec -T postgres psql -U eduflow -d eduflow
```

## Ajouter un nom de domaine (et le HTTPS)

1. Achète un domaine (chez Hostinger ou ailleurs), par exemple `eduflow-exemple.com`.
2. Dans la zone DNS du domaine, crée un enregistrement **A** : nom `@`, valeur = l'IP du VPS.
   Attends quelques minutes que le DNS se propage (`ping eduflow-exemple.com` doit répondre avec ton IP).
3. Dans `.env` :
   ```
   SITE_ADDRESS=eduflow-exemple.com
   PUBLIC_URL=https://eduflow-exemple.com
   ```
4. Relance :
   ```bash
   docker compose -f compose.prod.yaml up -d
   ```

Caddy obtient le certificat HTTPS (Let's Encrypt) tout seul et le renouvelle automatiquement.

## E-mails (invitations, mot de passe oublié)

Sans configuration, les e-mails sont seulement écrits dans les logs (`logs -f api`). Pour les envoyer
vraiment, crée un compte gratuit sur **Brevo** (300 e-mails/jour), récupère les identifiants SMTP et
décommente les lignes `SPRING_MAIL_*` et `APP_MAIL_FROM` dans `.env`, puis relance la plateforme.

## Dépannage

| Symptôme | Commande / piste |
|---|---|
| Un service redémarre en boucle | `docker compose -f compose.prod.yaml logs api` (ou `web`, `postgres`) |
| `set JWT_SECRET in .env` au lancement | une variable obligatoire est vide dans `.env` |
| `ADMIN_PASSWORD must be at least 10 characters` | mot de passe admin trop court |
| La page ne s'ouvre pas | pare-feu : ports 80/443 ouverts dans `ufw` **et** dans le panneau Hostinger |
| Erreur pendant la compilation Angular | essaie `cd ../education-platform-front && npm ci && npx ng build` pour voir l'erreur exacte |
| Disque plein | `docker system prune` supprime les anciennes images de build |
