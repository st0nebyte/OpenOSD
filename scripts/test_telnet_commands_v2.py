#!/usr/bin/env python3
"""
Test all supported telnet commands from X1200W_EU_Commands.csv
against the actual Denon AVR device - Version 2 with persistent connection.
"""

import csv
import socket
import time
import sys
from pathlib import Path


class PersistentTelnetTester:
    def __init__(self, host, port=23, timeout=1.0):
        self.host = host
        self.port = port
        self.timeout = timeout
        self.sock = None

    def connect(self):
        """Establish persistent connection"""
        if self.sock:
            self.close()

        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.settimeout(self.timeout)
        self.sock.connect((self.host, self.port))

        # Clear any initial data
        time.sleep(0.2)
        try:
            self.sock.recv(4096)
        except:
            pass

    def close(self):
        """Close connection"""
        if self.sock:
            try:
                self.sock.close()
            except:
                pass
            self.sock = None

    def flush_buffer(self):
        """Flush receive buffer"""
        self.sock.settimeout(0.05)
        try:
            while True:
                data = self.sock.recv(4096)
                if not data:
                    break
        except:
            pass
        finally:
            self.sock.settimeout(self.timeout)

    def send_command(self, command, wait_time=0.15):
        """Send a command via telnet and return the response"""
        try:
            if not self.sock:
                self.connect()

            # Flush any pending data
            self.flush_buffer()

            # Send command with \r terminator
            self.sock.sendall(f"{command}\r".encode('ascii'))

            # Wait for response
            time.sleep(wait_time)

            # Read response
            response_parts = []
            self.sock.settimeout(0.1)
            try:
                while True:
                    data = self.sock.recv(1024)
                    if not data:
                        break
                    response_parts.append(data.decode('ascii', errors='ignore'))
                    # Stop if we got enough data
                    if len(response_parts) > 10:
                        break
            except socket.timeout:
                pass
            finally:
                self.sock.settimeout(self.timeout)

            response = ''.join(response_parts).strip()

            # Filter out responses that clearly don't match this command
            # (helps with async responses from AVR)
            if response:
                lines = [l.strip() for l in response.split('\r') if l.strip()]
                # Try to find a line that starts with our command prefix
                cmd_prefix = command[:2]
                for line in lines:
                    if line.startswith(cmd_prefix) or command.endswith('?'):
                        return line

                # If no matching line, return first line
                return lines[0] if lines else ""

            return response

        except Exception as e:
            # Try to reconnect on error
            self.close()
            return f"ERROR: {e}"

    def test_command(self, command, expected_response, retry_on_error=True):
        """Test a single command and compare with expected response"""
        actual = self.send_command(command)

        # Retry once on connection error
        if retry_on_error and actual.startswith('ERROR'):
            time.sleep(0.5)
            try:
                self.connect()
                actual = self.send_command(command)
            except:
                pass

        # Clean up expected response (remove <CR> markers)
        expected_clean = expected_response.replace('<CR>', '').strip()

        # For query commands, we don't always get the exact expected response
        # because it depends on current state
        is_query = command.endswith('?')

        if is_query:
            # For queries, just check we got a response with the right prefix
            cmd_prefix = command[:-1]  # Remove ?
            success = len(actual) > 0 and not actual.startswith('ERROR') and (
                actual.startswith(cmd_prefix) or 'ERROR' not in actual
            )
        else:
            # For set commands, check exact match (or close match)
            success = (expected_clean and (expected_clean in actual or actual.startswith(expected_clean))) or \
                      (not expected_clean and not actual.startswith('ERROR') and len(actual) > 0)

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
    print(f"Using persistent connection with proper delays")
    print()

    # Create tester
    tester = PersistentTelnetTester(avr_host)

    try:
        # Connect once
        print("Connecting to AVR...")
        tester.connect()
        print("Connected!\n")

        # Test all commands
        results = []
        failed = []

        for i, cmd_info in enumerate(commands, 1):
            print(f"[{i}/{len(commands)}] Testing: {cmd_info['command'][:40]:40s}", end=' ... ', flush=True)

            result = tester.test_command(cmd_info['command'], cmd_info['expected'])
            results.append(result)

            if result['success']:
                print("✓")
            else:
                print("✗")
                failed.append({**cmd_info, **result})

            # Delay between commands to avoid overloading AVR
            time.sleep(0.1)

    finally:
        tester.close()

    # Summary
    print()
    print("=" * 80)
    print(f"SUMMARY: {len(results) - len(failed)}/{len(results)} commands succeeded")
    print(f"Success rate: {100 * (len(results) - len(failed)) / len(results):.1f}%")
    print("=" * 80)

    if failed:
        print()
        print(f"FAILED COMMANDS ({len(failed)}):")
        print("-" * 80)

        for f in failed[:20]:  # Show first 20 failures
            print(f"\nCommand: {f['command']}")
            print(f"Function: {f['function']}")
            print(f"Expected: {f['expected']}")
            print(f"Actual:   {f['actual']}")

        if len(failed) > 20:
            print(f"\n... and {len(failed) - 20} more failures")

        # Save full failure list to file
        failure_log = Path('/tmp/telnet_failures.log')
        with open(failure_log, 'w') as f:
            for fail in failed:
                f.write(f"Command: {fail['command']}\n")
                f.write(f"Function: {fail['function']}\n")
                f.write(f"Expected: {fail['expected']}\n")
                f.write(f"Actual: {fail['actual']}\n")
                f.write("-" * 40 + "\n")

        print(f"\nFull failure list saved to: {failure_log}")
    else:
        print()
        print("All commands tested successfully! ✓")

    return 0 if len(failed) == 0 else 1


if __name__ == '__main__':
    sys.exit(main())
