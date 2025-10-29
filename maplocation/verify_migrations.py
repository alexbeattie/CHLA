#!/usr/bin/env python3
"""Final verification that migrations work correctly"""
import os
import sys
import django

sys.path.insert(0, '/Users/alexbeattie/Developer/CHLA/maplocation')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'maplocation.settings')
django.setup()

from django.core.management import call_command
from io import StringIO

print("="*60)
print("FINAL MIGRATION VERIFICATION")
print("="*60)

# Run migrate
print("\nRunning: python3 manage.py migrate")
print("-"*60)
output = StringIO()
call_command('migrate', stdout=output, stderr=output, no_color=True)
print(output.getvalue())

# Show migrations
print("\n" + "="*60)
print("Migration Status:")
print("="*60)
output = StringIO()
call_command('showmigrations', 'locations', stdout=output, stderr=output, no_color=True)
print(output.getvalue())

print("\n" + "="*60)
print("✅ MIGRATIONS ARE WORKING CORRECTLY!")
print("="*60)

