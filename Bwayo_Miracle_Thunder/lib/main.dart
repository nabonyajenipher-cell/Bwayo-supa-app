import 'package:flutter/material.dart';

void main() => runApp(const BwayoMiracleThunder());

class BwayoMiracleThunder extends StatelessWidget {
  const BwayoMiracleThunder({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
    title: 'BWAYO MIRACLE THUNDER',
    debugShowCheckedModeBanner: false,
    theme: ThemeData(
      brightness: Brightness.dark,
      colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF16C7A2), brightness: Brightness.dark),
      scaffoldBackgroundColor: const Color(0xFF08131F),
      useMaterial3: true,
    ),
    home: const ThunderHome(),
  );
}

class ThunderHome extends StatefulWidget {
  const ThunderHome({super.key});
  @override
  State<ThunderHome> createState() => _ThunderHomeState();
}

class _ThunderHomeState extends State<ThunderHome> {
  int selected = 0;
  final pages = const [DashboardPage(), DevicePage(), DiagnosticsPage(), FirmwarePage(), BackupPage(), AdbGuidePage()];
  @override
  Widget build(BuildContext context) {
    const titles = ['Dashboard', 'Device information', 'Diagnostics', 'Firmware servicing', 'Backup & restore', 'ADB guide'];
    return Scaffold(
      appBar: AppBar(
        title: const Text('BWAYO MIRACLE THUNDER', style: TextStyle(fontWeight: FontWeight.w800, letterSpacing: .5)),
        actions: const [Padding(padding: EdgeInsets.only(right: 16), child: Chip(avatar: Icon(Icons.circle, color: Colors.orange, size: 10), label: Text('Not connected')))],
      ),
      body: Row(children: [
        NavigationRail(
          selectedIndex: selected,
          onDestinationSelected: (v) => setState(() => selected = v),
          labelType: NavigationRailLabelType.all,
          destinations: const [
            NavigationRailDestination(icon: Icon(Icons.dashboard_outlined), selectedIcon: Icon(Icons.dashboard), label: Text('Home')),
            NavigationRailDestination(icon: Icon(Icons.phone_android), label: Text('Device')),
            NavigationRailDestination(icon: Icon(Icons.health_and_safety_outlined), label: Text('Check')),
            NavigationRailDestination(icon: Icon(Icons.system_update_alt), label: Text('Firmware')),
            NavigationRailDestination(icon: Icon(Icons.backup_outlined), label: Text('Backup')),
            NavigationRailDestination(icon: Icon(Icons.terminal), label: Text('ADB guide')),
          ],
        ),
        const VerticalDivider(width: 1),
        Expanded(child: Padding(padding: const EdgeInsets.all(20), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(titles[selected], style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.bold)),
          const SizedBox(height: 16),
          Expanded(child: pages[selected]),
          const Divider(),
          const Row(children: [Icon(Icons.verified_user_outlined, size: 16, color: Colors.tealAccent), SizedBox(width: 8), Expanded(child: Text('Owner & Founder: BWAYO EMMANUEL', style: TextStyle(color: Colors.white70))), Text('Build 1.1.0', style: TextStyle(color: Colors.white38))]),
        ]))),
      ]),
    );
  }
}

class DashboardPage extends StatelessWidget {
  const DashboardPage({super.key});
  @override
  Widget build(BuildContext context) => ListView(children: [
    const Text('Phone servicing workspace', style: TextStyle(fontSize: 18, color: Colors.white70)),
    const SizedBox(height: 18),
    Wrap(spacing: 14, runSpacing: 14, children: const [
      InfoCard(icon: Icons.usb, title: 'USB connection', value: 'Waiting for device', detail: 'Connect an authorized Android phone'),
      InfoCard(icon: Icons.phone_android, title: 'Device details', value: 'Not available', detail: 'Device integration is not yet implemented'),
      InfoCard(icon: Icons.monitor_heart_outlined, title: 'Diagnostics', value: 'Ready', detail: 'Run the manual checks below'),
      InfoCard(icon: Icons.system_update_alt, title: 'Firmware', value: 'Preparation ready', detail: 'Official firmware workflow guidance'),
    ]),
    const SizedBox(height: 22),
    const SectionCard(title: 'Quick start', child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Bullet('Use a reliable USB data cable, not a charge-only cable.'),
      Bullet('Unlock the phone and approve only prompts you understand.'),
      Bullet('Confirm you have permission to service the device.'),
      Bullet('Back up important data before any reset or firmware operation.'),
    ])),
  ]);
}

class DevicePage extends StatelessWidget {
  const DevicePage({super.key});
  @override
  Widget build(BuildContext context) => ListView(children: const [
    SectionCard(title: 'Connection status', child: Text('No phone is currently detected. Live USB discovery is not yet connected to the platform layer.')),
    SectionCard(title: 'Information available after integration', child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Bullet('Manufacturer and model'), Bullet('Android version and build number'),
      Bullet('Serial number, where device and user permissions allow it'),
      Bullet('Battery and storage information, where supported'),
    ])),
    SectionCard(title: 'Connect safely', child: Text('Connect with a data-capable USB cable. Keep the screen unlocked and accept only prompts you understand.')),
  ]);
}

class DiagnosticsPage extends StatelessWidget {
  const DiagnosticsPage({super.key});
  @override
  Widget build(BuildContext context) => ListView(children: const [
    SectionCard(title: 'Manual diagnostic checklist', child: Column(children: [
      CheckRow('USB cable and port condition'), CheckRow('Battery level and charging response'),
      CheckRow('Touchscreen response'), CheckRow('Speaker, microphone and earpiece'),
      CheckRow('Camera and flash'), CheckRow('Wi-Fi, Bluetooth and mobile network'),
      CheckRow('Available storage and restart behaviour'),
    ])),
    SectionCard(title: 'Diagnostic note', child: Text('These checks are guidance only. Automated hardware tests require device-specific permissions and integration.')),
  ]);
}

class FirmwarePage extends StatefulWidget {
  const FirmwarePage({super.key});
  @override
  State<FirmwarePage> createState() => _FirmwarePageState();
}

class _FirmwarePageState extends State<FirmwarePage> {
  String brand = 'Samsung';
  final Map<String, bool> checks = {
    'I own this device or have clear permission to service it': false,
    'I confirmed the exact model and regional variant': false,
    'I downloaded firmware from the manufacturer or an authorized source': false,
    'My important data is backed up': false,
    'The battery is charged to at least 60%': false,
    'I understand that incorrect firmware can erase data or damage the device': false,
  };

  @override
  Widget build(BuildContext context) {
    final ready = checks.values.every((v) => v);
    return ListView(children: [
      Card(child: Padding(padding: const EdgeInsets.all(18), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        const Row(children: [Icon(Icons.system_update_alt, color: Colors.tealAccent, size: 28), SizedBox(width: 10), Expanded(child: Text('Firmware servicing', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)))]),
        const SizedBox(height: 10),
        const Text('Prepare an authorized firmware update using the phone maker’s official process. This screen is a preparation checklist; direct USB flashing is not yet implemented.'),
        const SizedBox(height: 16),
        DropdownButtonFormField<String>(
          value: brand,
          decoration: const InputDecoration(labelText: 'Device manufacturer', border: OutlineInputBorder()),
          items: const ['Samsung', 'Tecno', 'Infinix', 'Itel', 'Google Pixel', 'Motorola', 'Xiaomi', 'Other'].map((v) => DropdownMenuItem(value: v, child: Text(v))).toList(),
          onChanged: (v) => setState(() => brand = v ?? brand),
        ),
        const SizedBox(height: 16),
        const Text('Before proceeding', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 17)),
        const SizedBox(height: 8),
        ...checks.keys.map((label) => CheckboxListTile(
          value: checks[label],
          onChanged: (v) => setState(() => checks[label] = v ?? false),
          dense: true,
          contentPadding: EdgeInsets.zero,
          controlAffinity: ListTileControlAffinity.leading,
          title: Text(label),
        )),
        const SizedBox(height: 12),
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(color: Colors.orange.withValues(alpha: .12), borderRadius: BorderRadius.circular(10), border: Border.all(color: Colors.orangeAccent)),
          child: const Text('IMPORTANT: Never flash a package for a different model or region. Do not disconnect the cable during an official update. Firmware operations can erase all user data.', style: TextStyle(color: Colors.orangeAccent)),
        ),
        const SizedBox(height: 14),
        FilledButton.icon(
          onPressed: ready ? () => showDialog<void>(
            context: context,
            builder: (context) => AlertDialog(
              title: const Text('Preparation checklist complete'),
              content: Text('You selected $brand. Use the manufacturer’s official updater and follow its model-specific instructions. BWAYO MIRACLE THUNDER does not send firmware to the phone yet.'),
              actions: [TextButton(onPressed: () => Navigator.pop(context), child: const Text('OK'))],
            ),
          ) : null,
          icon: const Icon(Icons.verified_outlined),
          label: const Text('Confirm preparation checklist'),
        ),
        const SizedBox(height: 8),
        Text(ready ? 'Checklist complete — official updater still required.' : 'Complete all six checks to confirm preparation.', style: const TextStyle(color: Colors.white70)),
      ]))),
      const SectionCard(title: 'Supported approach', child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Bullet('Use the manufacturer’s official recovery or firmware utility for the exact device model.'),
        Bullet('Verify firmware model, region, source and integrity before use.'),
        Bullet('Keep the phone powered and connected throughout the official process.'),
        Bullet('If the phone is managed by an organization, obtain its authorization first.'),
      ])),
      const SectionCard(title: 'Not provided by this app', child: Text('No FRP or screen-lock bypass, account bypass, IMEI modification, bootloader security circumvention, or forced/unsupported flashing. Live flashing needs a tested, model-specific integration and official firmware source.')),
    ]);
  }
}

class BackupPage extends StatelessWidget {
  const BackupPage({super.key});
  @override
  Widget build(BuildContext context) => ListView(children: const [
    SectionCard(title: 'Before servicing', child: Column(children: [
      CheckRow('Ask the owner to back up photos and documents'),
      CheckRow('Confirm contacts and messages are backed up'),
      CheckRow('Record the device condition and reported fault'),
      CheckRow('Confirm the owner understands possible data loss'),
    ])),
    SectionCard(title: 'Restore guidance', child: Text('Restore data using the phone maker’s official backup and restore process. This starter does not copy private data automatically.')),
  ]);
}

class AdbGuidePage extends StatelessWidget {
  const AdbGuidePage({super.key});
  @override
  Widget build(BuildContext context) => ListView(children: const [
    SectionCard(title: 'ADB setup', child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Bullet('Install Android Platform Tools from the official Android developer website.'),
      Bullet('Enable Developer options only with the device owner’s consent.'),
      Bullet('Enable USB debugging only when needed for an authorized task.'),
      Bullet('Verify the computer’s RSA authorization prompt on the phone.'),
    ])),
    SectionCard(title: 'Safe read-only examples', child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      CodeLine('adb devices'), CodeLine('adb shell getprop ro.product.model'),
      CodeLine('adb shell getprop ro.build.version.release'),
    ])),
    SectionCard(title: 'Not included', child: Text('No security-lock or FRP bypass, IMEI modification, account removal, or unauthorized firmware flashing. Vendor-specific operations need official packages and validated procedures.')),
  ]);
}

class InfoCard extends StatelessWidget {
  final IconData icon; final String title; final String value; final String detail;
  const InfoCard({super.key, required this.icon, required this.title, required this.value, required this.detail});
  @override
  Widget build(BuildContext context) => SizedBox(width: 235, child: Card(child: Padding(
    padding: const EdgeInsets.all(18),
    child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Icon(icon, color: Colors.tealAccent, size: 28), const SizedBox(height: 12),
      Text(title, style: const TextStyle(color: Colors.white70)), const SizedBox(height: 6),
      Text(value, style: const TextStyle(fontSize: 19, fontWeight: FontWeight.bold)),
      const SizedBox(height: 6), Text(detail, style: const TextStyle(color: Colors.white54)),
    ]),
  )));
}

class SectionCard extends StatelessWidget {
  final String title; final Widget child;
  const SectionCard({super.key, required this.title, required this.child});
  @override
  Widget build(BuildContext context) => Card(margin: const EdgeInsets.only(bottom: 14), child: Padding(
    padding: const EdgeInsets.all(18), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Text(title, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 17)),
      const SizedBox(height: 12), child,
    ]),
  ));
}
class Bullet extends StatelessWidget {
  final String text;
  const Bullet(this.text, {super.key});
  @override
  Widget build(BuildContext context) => Padding(padding: const EdgeInsets.symmetric(vertical: 5), child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
    const Icon(Icons.check_circle_outline, color: Colors.tealAccent, size: 18), const SizedBox(width: 9), Expanded(child: Text(text)),
  ]));
}
class CheckRow extends StatelessWidget {
  final String text;
  const CheckRow(this.text, {super.key});
  @override
  Widget build(BuildContext context) => CheckboxListTile(value: false, onChanged: null, dense: true, contentPadding: EdgeInsets.zero, title: Text(text));
}
class CodeLine extends StatelessWidget {
  final String text;
  const CodeLine(this.text, {super.key});
  @override
  Widget build(BuildContext context) => Container(width: double.infinity, margin: const EdgeInsets.symmetric(vertical: 4), padding: const EdgeInsets.all(12), decoration: BoxDecoration(color: Colors.black26, borderRadius: BorderRadius.circular(8)), child: SelectableText(text, style: const TextStyle(fontFamily: 'monospace', color: Colors.tealAccent)));
}
