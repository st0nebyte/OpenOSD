#!/usr/bin/env python3
"""
Test all supported telnet commands from X1200W_EU_Commands.csv
against the actual Denon AVR device.
"""

import csv
import socket
import time
import sys
from pathlib import Path


class TelnetTester:
    def __init__(self, host, port=23, timeout=2.0):
        self.host = host
        self.port = port
        self.timeout = timeout

    def send_command(self, command):
        """Send a command via telnet and return the response"""
        try:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
                sock.settimeout(self.timeout)
                sock.connect((self.host, self.port))

                # Send command with \r terminator
                sock.sendall(f"{command}\r".encode('ascii'))

                # Read response (wait a bit for response)
                time.sleep(0.1)

                try:
                    response = sock.recv(4096).decode('ascii', errors='ignore')
                    return response.strip()
                except socket.timeout:
                    return ""

        except Exception as e:
            return f"ERROR: {e}"

    def test_command(self, command, expected_response):
        """Test a single command and compare with expected response"""
        actual = self.send_command(command)

        # Clean up expected response (remove <CR> markers)
        expected_clean = expected_response.replace('<CR>', '').strip()

        # For query commands, we don't always get the exact expected response
        # because it depends on current state
        is_query = command.endswith('?')

        if is_query:
            # For queries, just check we got a response
            success = len(actual) > 0 and not actual.startswith('ERROR')
        else:
            # For set commands, check exact match (or close match)
            success = expected_clean in actual or actual in expected_clean or actual.startswith(expected_clean)

        return {
            'command': command,
            'expected': expected_clean,
            'actual': actual,
            'success': success,
            'is_query': is_query
        }


def parse_csv(csv_path):
    """Parse the CSV and return list of supported commands to test"""
    commands = []

    with open(csv_path, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)

        for row in reader:
            # Skip empty rows
            if not row or all(v is None or v == '' for v in row.values()):
                continue

            # Skip if not supported
            supported = row.get('Supported_X1200W_EU', '') or ''
            if supported.strip() != '✔':
                continue

            # Extract command from Example column
            example = (row.get('Example', '') or '').strip()
            if not example:
                continue

            # Remove <CR> to get the actual command
            command = example.replace('<CR>', '').strip()

            if not command:
                continue

            # Get expected response
            response = (row.get('Response', '') or '').strip()

            commands.append({
                'command': command,
                'expected': response,
                'function': (row.get('Function', '') or '').strip(),
                'prefix': (row.get('Command', '') or '').strip(),
                'parameter': (row.get('Parameter', '') or '').strip()
            })

    return commands


def main():
    # Configuration
    avr_host = '192.168.178.130'
    csv_path = Path(__file__).parent.parent / 'X1200W_EU_Commands.csv'

    print(f"Testing Denon AVR at {avr_host}")
    print(f"Loading commands from {csv_path}")
    print()

    # Parse CSV
    commands = parse_csv(csv_path)
    print(f"Found {len(commands)} supported commands to test")
    print()

    # Create tester
    tester = TelnetTester(avr_host)

    # Test all commands
    results = []
    failed = []

    for i, cmd_info in enumerate(commands, 1):
        print(f"[{i}/{len(commands)}] Testing: {cmd_info['command'][:40]:40s}", end=' ... ')

        result = tester.test_command(cmd_info['command'], cmd_info['expected'])
        results.append(result)

        if result['success']:
            print("✓")
        else:
            print("✗")
            failed.append({**cmd_info, **result})

        # Small delay between commands
        time.sleep(0.05)

    # Summary
    print()
    print("=" * 80)
    print(f"SUMMARY: {len(results) - len(failed)}/{len(results)} commands succeeded")
    print("=" * 80)

    if failed:
        print()
        print(f"FAILED COMMANDS ({len(failed)}):")
        print("-" * 80)

        for f in failed:
            print(f"\nCommand: {f['command']}")
            print(f"Function: {f['function']}")
            print(f"Expected: {f['expected']}")
            print(f"Actual:   {f['actual']}")
    else:
        print()
        print("All commands tested successfully! ✓")

    return 0 if len(failed) == 0 else 1


if __name__ == '__main__':
    sys.exit(main())
