import 'package:drift/drift.dart';
import '../app_database.dart';
import '../tables.dart';

part 'shortcut_dao.g.dart';

class ShortcutWithTags {
  final Shortcut shortcut;
  final List<Tag> tags;

  ShortcutWithTags(this.shortcut, this.tags);
}

@DriftAccessor(tables: [Shortcuts, Tags, ShortcutTags])
class ShortcutDao extends DatabaseAccessor<AppDatabase>
    with _$ShortcutDaoMixin {
  ShortcutDao(super.db);

  Stream<List<ShortcutWithTags>> watchAllWithTags() {
    return select(shortcuts).watch().asyncMap((allShortcuts) {
      return Future.wait(allShortcuts.map((s) async {
        final tagList = await _getTagsForShortcut(s.id);
        return ShortcutWithTags(s, tagList);
      }));
    });
  }

  Future<ShortcutWithTags?> getByIdWithTags(int id) async {
    final shortcut = await (select(shortcuts)
          ..where((t) => t.id.equals(id)))
        .getSingleOrNull();
    if (shortcut == null) return null;
    final tagList = await _getTagsForShortcut(id);
    return ShortcutWithTags(shortcut, tagList);
  }

  Future<int> insertShortcut(ShortcutsCompanion entry) =>
      into(shortcuts).insert(entry);

  Future<void> updateShortcut(ShortcutsCompanion entry) =>
      (update(shortcuts)..where((t) => t.id.equals(entry.id.value)))
          .write(entry);

  Future<void> deleteShortcut(int id) =>
      (delete(shortcuts)..where((t) => t.id.equals(id))).go();

  Future<void> updateLastUsed(int id) =>
      (update(shortcuts)..where((t) => t.id.equals(id))).write(
        ShortcutsCompanion(
            lastUsed: Value(DateTime.now().millisecondsSinceEpoch)),
      );

  Future<void> setTagsForShortcut(int shortcutId, List<String> tagIds) {
    return transaction(() async {
      await (delete(shortcutTags)
            ..where((t) => t.shortcutId.equals(shortcutId)))
          .go();
      for (final tagId in tagIds) {
        await into(shortcutTags).insert(
          ShortcutTagsCompanion(
            shortcutId: Value(shortcutId),
            tagId: Value(tagId),
          ),
        );
      }
    });
  }

  Future<List<Tag>> _getTagsForShortcut(int shortcutId) async {
    final tagIds = await (select(shortcutTags)
          ..where((t) => t.shortcutId.equals(shortcutId)))
        .map((row) => row.tagId)
        .get();
    if (tagIds.isEmpty) return [];
    return (select(tags)..where((t) => t.id.isIn(tagIds))).get();
  }

}