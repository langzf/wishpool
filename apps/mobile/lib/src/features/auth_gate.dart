import 'package:flutter/material.dart';

import '../auth/session.dart';
import '../data/auth_repository.dart';
import '../data/wishpool_scope.dart';
import '../design/wishpool_theme.dart';
import '../infrastructure/auth_session_store.dart';
import '../infrastructure/runtime_config.dart';
import '../infrastructure/wishpool_api_client.dart';
import 'mobile_home.dart';

class AuthGate extends StatefulWidget {
  const AuthGate({super.key});

  @override
  State<AuthGate> createState() => _AuthGateState();
}

class _AuthGateState extends State<AuthGate> {
  final _store = const AuthSessionStore();
  late final WishPoolRuntimeConfig _baseConfig;
  late final AuthRepository _authRepository;
  WishPoolSession? _session;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _baseConfig = WishPoolRuntimeConfig.local;
    _authRepository = AuthRepository(
      config: _baseConfig,
      apiClient: WishPoolApiClient(config: _baseConfig),
    );
    _restore();
  }

  @override
  Widget build(BuildContext context) {
    final session = _session;
    if (_loading) return const _LoadingScreen();
    if (session == null) {
      return AuthScreen(
        repository: _authRepository,
        onAuthenticated: _saveSession,
      );
    }
    if (!session.hasFamily || !session.hasChild) {
      return FamilySetupScreen(
        session: session,
        repository: _authRepository,
        onCompleted: _saveSession,
        onSignOut: _signOut,
      );
    }

    return WishPoolScope(
      config: _baseConfig.copyWith(
        useRemoteApi: true,
        accessToken: session.accessToken,
        familyId: session.familyId,
        childId: session.childId,
        role: session.role,
      ),
      child: MobileHomeScreen(
        childMode: session.isChildDevice,
        onSignOut: _signOut,
      ),
    );
  }

  Future<void> _restore() async {
    final stored = await _store.load();
    final envSession = _sessionFromEnv();
    if (stored == null && envSession == null) {
      setState(() => _loading = false);
      return;
    }
    try {
      final restored = stored == null ? await _authRepository.enrichSession(envSession!) : await _authRepository.refresh(stored);
      await _saveSession(restored);
    } catch (_) {
      await _store.clear();
      if (!mounted) return;
      setState(() => _loading = false);
    }
  }

  WishPoolSession? _sessionFromEnv() {
    if (!_baseConfig.hasRemoteContext) return null;
    return WishPoolSession(
      accessToken: _baseConfig.accessToken,
      refreshToken: '',
      familyId: _baseConfig.familyId,
      childId: _baseConfig.childId,
      role: 'parent',
      displayName: 'WishPool',
    );
  }

  Future<void> _saveSession(WishPoolSession session) async {
    await _store.save(session);
    if (!mounted) return;
    setState(() {
      _session = session;
      _loading = false;
    });
  }

  Future<void> _signOut() async {
    await _store.clear();
    if (!mounted) return;
    setState(() {
      _session = null;
      _loading = false;
    });
  }
}

class AuthScreen extends StatefulWidget {
  const AuthScreen({
    super.key,
    required this.repository,
    required this.onAuthenticated,
  });

  final AuthRepository repository;
  final ValueChanged<WishPoolSession> onAuthenticated;

  @override
  State<AuthScreen> createState() => _AuthScreenState();
}

class _AuthScreenState extends State<AuthScreen> {
  final _phoneController = TextEditingController();
  final _codeController = TextEditingController();
  final _pairingController = TextEditingController();
  String? _verificationToken;
  String? _debugCode;
  bool _childMode = false;
  bool _submitting = false;

  @override
  void dispose() {
    _phoneController.dispose();
    _codeController.dispose();
    _pairingController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 32, 20, 32),
          children: [
            Text('WishPool', style: theme.textTheme.headlineLarge),
            const SizedBox(height: WishPoolSpacing.xs),
            Text(_childMode ? '绑定儿童设备' : '家长手机号登录', style: theme.textTheme.titleLarge),
            const SizedBox(height: WishPoolSpacing.lg),
            SegmentedButton<bool>(
              segments: const [
                ButtonSegment(value: false, icon: Icon(Icons.phone_iphone), label: Text('家长')),
                ButtonSegment(value: true, icon: Icon(Icons.child_care), label: Text('儿童')),
              ],
              selected: {_childMode},
              onSelectionChanged: (value) => setState(() => _childMode = value.first),
            ),
            const SizedBox(height: WishPoolSpacing.lg),
            if (_childMode) ...[
              TextField(
                controller: _pairingController,
                decoration: const InputDecoration(
                  border: OutlineInputBorder(),
                  labelText: '配对码',
                  prefixIcon: Icon(Icons.key_outlined),
                ),
              ),
              const SizedBox(height: WishPoolSpacing.md),
              FilledButton.icon(
                onPressed: _submitting ? null : _consumePairingCode,
                icon: const Icon(Icons.link_outlined),
                label: const Text('绑定设备'),
              ),
            ] else ...[
              TextField(
                controller: _phoneController,
                keyboardType: TextInputType.phone,
                decoration: const InputDecoration(
                  border: OutlineInputBorder(),
                  labelText: '手机号',
                  prefixIcon: Icon(Icons.phone_outlined),
                ),
              ),
              const SizedBox(height: WishPoolSpacing.sm),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _codeController,
                      keyboardType: TextInputType.number,
                      decoration: const InputDecoration(
                        border: OutlineInputBorder(),
                        labelText: '验证码',
                        prefixIcon: Icon(Icons.password_outlined),
                      ),
                    ),
                  ),
                  const SizedBox(width: WishPoolSpacing.sm),
                  OutlinedButton(
                    onPressed: _submitting ? null : _requestPhoneCode,
                    child: const Text('获取'),
                  ),
                ],
              ),
              if (_debugCode != null) ...[
                const SizedBox(height: WishPoolSpacing.xs),
                Text('本地验证码：$_debugCode', style: theme.textTheme.bodyMedium),
              ],
              const SizedBox(height: WishPoolSpacing.md),
              FilledButton.icon(
                onPressed: _submitting ? null : _loginWithPhone,
                icon: const Icon(Icons.login_outlined),
                label: const Text('登录'),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Future<void> _requestPhoneCode() async {
    await _run(() async {
      final code = await widget.repository.requestPhoneCode(_phoneController.text);
      setState(() {
        _verificationToken = code.verificationToken;
        _debugCode = code.debugCode;
      });
    });
  }

  Future<void> _loginWithPhone() async {
    final token = _verificationToken;
    if (token == null || token.isEmpty) {
      _showMessage('请先获取验证码。');
      return;
    }
    await _run(() async {
      final session = await widget.repository.loginWithPhone(
        verificationToken: token,
        code: _codeController.text.trim(),
      );
      widget.onAuthenticated(session);
    });
  }

  Future<void> _consumePairingCode() async {
    await _run(() async {
      final session = await widget.repository.pairChildDevice(_pairingController.text);
      widget.onAuthenticated(session);
    });
  }

  Future<void> _run(Future<void> Function() action) async {
    setState(() => _submitting = true);
    try {
      await action();
    } catch (_) {
      _showMessage('操作失败，请检查输入和本地服务。');
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  void _showMessage(String message) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }
}

class FamilySetupScreen extends StatefulWidget {
  const FamilySetupScreen({
    super.key,
    required this.session,
    required this.repository,
    required this.onCompleted,
    required this.onSignOut,
  });

  final WishPoolSession session;
  final AuthRepository repository;
  final ValueChanged<WishPoolSession> onCompleted;
  final VoidCallback onSignOut;

  @override
  State<FamilySetupScreen> createState() => _FamilySetupScreenState();
}

class _FamilySetupScreenState extends State<FamilySetupScreen> {
  final _familyController = TextEditingController(text: '星愿小屋');
  final _childController = TextEditingController();
  final _timezoneController = TextEditingController(text: 'Asia/Shanghai');
  bool _submitting = false;

  @override
  void dispose() {
    _familyController.dispose();
    _childController.dispose();
    _timezoneController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 32, 20, 32),
          children: [
            Text('创建家庭空间', style: theme.textTheme.headlineLarge),
            const SizedBox(height: WishPoolSpacing.xs),
            Text('欢迎，${widget.session.displayName}', style: theme.textTheme.bodyLarge),
            const SizedBox(height: WishPoolSpacing.lg),
            TextField(
              controller: _familyController,
              decoration: const InputDecoration(
                border: OutlineInputBorder(),
                labelText: '家庭名称',
                prefixIcon: Icon(Icons.home_outlined),
              ),
            ),
            const SizedBox(height: WishPoolSpacing.sm),
            TextField(
              controller: _childController,
              decoration: const InputDecoration(
                border: OutlineInputBorder(),
                labelText: '孩子昵称',
                prefixIcon: Icon(Icons.face_6_outlined),
              ),
            ),
            const SizedBox(height: WishPoolSpacing.sm),
            TextField(
              controller: _timezoneController,
              decoration: const InputDecoration(
                border: OutlineInputBorder(),
                labelText: '时区',
                prefixIcon: Icon(Icons.schedule_outlined),
              ),
            ),
            const SizedBox(height: WishPoolSpacing.md),
            FilledButton.icon(
              onPressed: _submitting ? null : _createFamily,
              icon: const Icon(Icons.check_circle_outline),
              label: const Text('进入家庭'),
            ),
            const SizedBox(height: WishPoolSpacing.sm),
            OutlinedButton.icon(
              onPressed: widget.onSignOut,
              icon: const Icon(Icons.logout_outlined),
              label: const Text('退出登录'),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _createFamily() async {
    if (_familyController.text.trim().isEmpty || _childController.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('请填写家庭名称和孩子昵称。')));
      return;
    }
    setState(() => _submitting = true);
    try {
      final session = await widget.repository.createFamilyWithChild(
        session: widget.session,
        familyName: _familyController.text.trim(),
        childName: _childController.text.trim(),
        timezone: _timezoneController.text.trim().isEmpty ? 'Asia/Shanghai' : _timezoneController.text.trim(),
      );
      widget.onCompleted(session);
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('创建失败，请确认本地服务已启动。')));
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }
}

class _LoadingScreen extends StatelessWidget {
  const _LoadingScreen();

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      body: Center(child: CircularProgressIndicator()),
    );
  }
}
