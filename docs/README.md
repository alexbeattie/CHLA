# CHLA Provider Map - Documentation Index

Welcome to the CHLA Provider Map documentation! This directory contains all project documentation organized for easy navigation.

## Quick Links

- **Getting Started**: [Backend Setup](../maplocation/README.md) | [Main Project README](../README.md)
- **Deployment**: [Deployment Guide](./DEPLOYMENT.md)
- **CI/CD**: [GitHub Actions Guide](./GITHUB_ACTIONS.md)
- **Database**: [Database Sync Guide](./DATABASE_SYNC.md)
- **Architecture**: [Stack Documentation](../STACK_DOCUMENTATION.md)

---

## Documentation Structure

### Core Documentation

| Document | Description | For |
|----------|-------------|-----|
| [DEPLOYMENT.md](./DEPLOYMENT.md) | Complete deployment guide for local and production | DevOps, Developers |
| [GITHUB_ACTIONS.md](./GITHUB_ACTIONS.md) | GitHub Actions CI/CD setup and troubleshooting | DevOps, Developers |
| [DATABASE_SYNC.md](./DATABASE_SYNC.md) | Database synchronization between local and RDS | Developers, DBAs |
| [../maplocation/README.md](../maplocation/README.md) | Backend getting started guide | New Developers |
| [../README.md](../README.md) | Main project overview and quick start | Everyone |
| [../STACK_DOCUMENTATION.md](../STACK_DOCUMENTATION.md) | Complete architecture and stack details | Architects, Senior Devs |

### Archived Documentation

Old, outdated, or superseded documentation has been moved to [archive/](./archive/) for reference:

- `DEPLOYMENT.md` (old) → Replaced by new `docs/DEPLOYMENT.md`
- `DEPLOYMENT_GUIDE.md` (old) → Consolidated into `docs/DEPLOYMENT.md`
- `DEPLOYMENT_CHECKLIST.md` (old) → Integrated into `docs/DEPLOYMENT.md`
- `GITHUB_ACTIONS_SETUP.md` (old) → Replaced by `docs/GITHUB_ACTIONS.md`
- `GITHUB_ACTIONS_FIXES.md` (old) → Integrated into `docs/GITHUB_ACTIONS.md`
- `GITHUB_SECRETS_SETUP.md` (old) → Part of `docs/GITHUB_ACTIONS.md`
- `manual_sync_commands.md` (old) → Replaced by `docs/DATABASE_SYNC.md`
- `ADMIN_SECURITY_OPTIONS.md` (old) → Integrated into backend README
- `QUICK_ADMIN_SECURITY.md` (old) → Integrated into backend README
- `AUTHENTICATION_GUIDE.md`, `MOBILE_CONTROLS_FIX.md`, `NOTIFICATION_SETUP.md`, etc.

---

## Documentation by Role

### For New Developers

Start here to get up and running:

1. [Main README](../README.md) - Project overview
2. [Backend Getting Started](../maplocation/README.md) - Setup backend
3. [Stack Documentation](../STACK_DOCUMENTATION.md) - Understand architecture

### For Developers

Day-to-day development guides:

- [Database Sync Guide](./DATABASE_SYNC.md) - Manage migrations and data
- [Deployment Guide](./DEPLOYMENT.md) - Deploy your changes
- [GitHub Actions Guide](./GITHUB_ACTIONS.md) - Understand CI/CD

### For DevOps/SRE

Infrastructure and deployment:

- [Deployment Guide](./DEPLOYMENT.md) - Production deployment
- [GitHub Actions Guide](./GITHUB_ACTIONS.md) - CI/CD pipeline
- [Stack Documentation](../STACK_DOCUMENTATION.md) - AWS resources
- [Database Sync Guide](./DATABASE_SYNC.md) - Database management

### For Architects

High-level architecture and decisions:

- [Stack Documentation](../STACK_DOCUMENTATION.md) - Complete architecture
- [Main README](../README.md) - Feature overview
- [Backend README](../maplocation/README.md) - Backend structure

---

## Documentation by Task

### Setting Up Development Environment

1. Read [Backend README](../maplocation/README.md)
2. Install prerequisites (Python 3.12, PostgreSQL, Node.js)
3. Set up environment variables
4. Run migrations
5. Start development servers

### Deploying to Production

1. Read [Deployment Guide](./DEPLOYMENT.md)
2. Verify GitHub secrets configured ([GitHub Actions Guide](./GITHUB_ACTIONS.md))
3. Test locally
4. Push to main branch (triggers auto-deploy)
5. Monitor deployment in GitHub Actions

### Managing Database Changes

1. Read [Database Sync Guide](./DATABASE_SYNC.md)
2. Create migrations locally: `python manage.py makemigrations`
3. Test migrations: `python manage.py migrate`
4. Commit and push (triggers auto-deploy with migrations)
5. Verify on production

### Troubleshooting Deployments

1. Check [Deployment Guide - Troubleshooting](./DEPLOYMENT.md#troubleshooting)
2. Review [GitHub Actions Guide - Troubleshooting](./GITHUB_ACTIONS.md#troubleshooting)
3. Check GitHub Actions logs
4. Review EB logs: `eb logs`
5. Verify environment variables

### Understanding the Stack

1. Read [Main README](../README.md) - Overview
2. Read [Stack Documentation](../STACK_DOCUMENTATION.md) - Detailed architecture
3. Review [Backend README](../maplocation/README.md) - Backend specifics
4. Check AWS Console for live resources

---

## Document Maintenance

### Adding New Documentation

1. Create document in appropriate location:
   - General guides → `/docs/`
   - Backend-specific → `/maplocation/docs/`
   - Frontend-specific → `/map-frontend/docs/` (if needed)
2. Update this index
3. Link from relevant documents
4. Add to version control

### Updating Documentation

1. Update the document
2. Add changelog note if major changes
3. Update this index if title/purpose changes
4. Move old version to archive/ if completely replaced

### Archiving Documentation

1. Move outdated docs to `docs/archive/`
2. Update this index to show new replacement
3. Keep for historical reference
4. Never delete (version control preserves history anyway)

---

## Documentation Standards

### File Naming

- Use `UPPERCASE_SNAKE_CASE.md` for major guides
- Use descriptive names: `DATABASE_SYNC.md` not `DB.md`
- Avoid generic names: `DEPLOYMENT.md` not `GUIDE.md`

### Document Structure

All major guides should include:

1. **Title and brief description**
2. **Quick Reference** - Common commands/links
3. **Table of Contents**
4. **Main Content** with clear sections
5. **Troubleshooting** section
6. **Additional Resources** - Links to related docs

### Markdown Style

- Use headings hierarchically (H1 → H2 → H3)
- Include code blocks with language tags
- Use tables for comparison/reference data
- Add links to related documentation
- Include examples where helpful

---

## Getting Help

If you can't find what you need in the documentation:

1. Check the [archive/](./archive/) for historical context
2. Review commit history for recent changes
3. Check GitHub Issues for known problems
4. Contact the development team

---

## Contributing to Documentation

Documentation improvements are always welcome!

1. Find outdated or unclear content
2. Make improvements
3. Test any commands/instructions
4. Update this index if needed
5. Submit pull request with clear description

**Good documentation helps everyone!**

---

## Quick Command Reference

### Local Development

```bash
# Backend
cd maplocation && python manage.py runserver

# Frontend
cd map-frontend && npm run dev
```

### Deployment

```bash
# Automatic (recommended)
git push origin main

# Manual backend
eb deploy chla-api-prod --region us-west-2

# Manual frontend
cd map-frontend && npm run build
aws s3 sync dist/ s3://kinddhelp-frontend-1755148345 --delete
aws cloudfront create-invalidation --distribution-id E2W6EECHUV4LMM --paths "/*"
```

### Database

```bash
# Create migrations
python manage.py makemigrations

# Apply migrations
python manage.py migrate

# Check status
python manage.py showmigrations
```

### Monitoring

```bash
# Check EB status
eb status --region us-west-2

# View logs
eb logs --region us-west-2

# GitHub Actions
# Visit: https://github.com/YOUR_USERNAME/CHLAProj/actions
```

---

**Last Updated**: 2024-10-26
**Maintained By**: Development Team
