import 'package:flutter/material.dart';
import 'package:without_ffmpeg/ffmpeg_kit.dart';
import 'package:without_ffmpeg/ffmpeg_kit_config.dart';
import 'package:without_ffmpeg/return_code.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _output = 'Tap the button to run ffmpeg -version';

  Future<void> _runFfmpegVersion() async {
    setState(() => _output = 'Running...');
    await FFmpegKitConfig.enableRedirection();
    final session = await FFmpegKit.execute('-version');
    final returnCode = await session.getReturnCode();
    final logs = await session.getAllLogsAsString();
    if (!mounted) return;
    setState(() {
      if (ReturnCode.isSuccess(returnCode)) {
        _output = logs ?? '(no output)';
      } else {
        _output = 'Failed: rc=${returnCode?.getValue()}\n$logs';
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('without_ffmpeg example')),
        body: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              FilledButton(
                onPressed: _runFfmpegVersion,
                child: const Text('ffmpeg -version'),
              ),
              const SizedBox(height: 16),
              Expanded(
                child: SingleChildScrollView(
                  child: Text(_output),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
