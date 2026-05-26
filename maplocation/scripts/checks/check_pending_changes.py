#!/usr/bin/env python3
"""
Check what model changes are pending that need a migration.
"""

import os
import sys
import django

sys.path.insert(0, '/Users/alexbeattie/Developer/CHLA/maplocation')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'maplocation.settings')

output_file = '/Users/alexbeattie/Developer/CHLA/maplocation/pending_changes_output.txt'

def log(message):
    print(message)
    with open(output_file, 'a') as f:
        f.write(message + '\n')

with open(output_file, 'w') as f:
    f.write(f"Pending Migration Changes - {__import__('datetime').datetime.now()}\n")
    f.write("="*60 + "\n\n")

try:
    log("Setting up Django...")
    django.setup()
    log("✓ Django setup complete")
    
    from django.core.management import call_command
    from io import StringIO
    
    log("\n" + "="*60)
    log("CHECKING FOR MODEL CHANGES")
    log("="*60 + "\n")
    
    # Check if makemigrations would create any migrations
    output = StringIO()
    call_command('makemigrations', '--dry-run', '--verbosity=2', stdout=output, stderr=output, no_color=True)
    log(output.getvalue())
    
    log("\n" + "="*60)
    log("DONE")
    log("="*60)
    
except Exception as e:
    log(f"\n❌ ERROR: {str(e)}")
    import traceback
    log("\nFull traceback:")
    log(traceback.format_exc())
    sys.exit(1)

