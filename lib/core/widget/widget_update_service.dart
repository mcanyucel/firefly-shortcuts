import 'dart:convert';
import 'package:flutter/services.dart';
import '../auth/auth_manager.dart';
import '../database/daos/shortcut_dao.dart';
import '../settings/settings_repository.dart';

class WidgetUpdateService {
  static const _channel = MethodChannel('firefly_shortcuts/widget');

  static Future<void> update({
    required ShortcutDao shortcutDao,
    required SettingsRepository settings,
    required AuthManager authManager,
  }) async {
    try {
      final details = await shortcutDao.watchAllWithDetails().first;
      final shortcutsJson = jsonEncode(
        details.map((d) => {
          'id': d.shortcut.id,
          'name': d.shortcut.name,
          'amount': d.shortcut.amount,
          'type': d.shortcut.transactionType.apiValue,
          'description': d.shortcut.description ?? d.shortcut.name,
          'fromAccountId': d.fromAccount.id,
          'fromAccountName': d.fromAccount.name,
          'toAccountId': d.toAccount.id,
          'toAccountName': d.toAccount.name,
          'categoryId': d.shortcut.categoryId,
          'budgetId': d.shortcut.budgetId,
          'billId': d.shortcut.billId,
          'piggybankId': d.shortcut.piggybankId,
          'tags': d.tags.map((t) => t.name).toList(),
        }).toList(),
      );

      await _channel.invokeMethod('updateWidgetData', {
        'shortcuts': shortcutsJson,
        'server_url': await settings.getServerUrl() ?? '',
        'access_token': authManager.state.accessToken ?? '',
      });
    } catch (_) {}
  }
}
