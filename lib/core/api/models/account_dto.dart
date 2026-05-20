  class AccountDto {
    final String id;
    final String name;
    final String accountType;
    final String? accountRole;
    final String? currencyCode;
    final String? currencySymbol;
    final bool active;

    const AccountDto({
      required this.id,
      required this.name,
      required this.accountType,
      this.accountRole,
      this.currencyCode,
      this.currencySymbol,
      required this.active,
    });

    factory AccountDto.fromJson(Map<String, dynamic> json) {
      final attrs = json['attributes'] as Map<String, dynamic>;
      return AccountDto(
        id: json['id'] as String,
        name: attrs['name'] as String,
        accountType: attrs['type'] as String,
        accountRole: attrs['account_role'] as String?,
        currencyCode: attrs['currency_code'] as String?,
        currencySymbol: attrs['currency_symbol'] as String?,
        active: attrs['active'] as bool,
      );
    }
  }