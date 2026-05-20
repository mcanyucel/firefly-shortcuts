class BillDto {
  final String id;
  final String name;
  const BillDto({required this.id, required this.name});
  factory BillDto.fromJson(Map<String, dynamic> json) => BillDto(
    id: json['id'] as String,
    name: (json['attributes'] as Map<String, dynamic>)['name'] as String,
  );
}

class BudgetDto {
  final String id;
  final String name;
  const BudgetDto({required this.id, required this.name});
  factory BudgetDto.fromJson(Map<String, dynamic> json) => BudgetDto(
    id: json['id'] as String,
    name: (json['attributes'] as Map<String, dynamic>)['name'] as String,
  );
}

class CategoryDto {
  final String id;
  final String name;
  const CategoryDto({required this.id, required this.name});
  factory CategoryDto.fromJson(Map<String, dynamic> json) => CategoryDto(
    id: json['id'] as String,
    name: (json['attributes'] as Map<String, dynamic>)['name'] as String,
  );
}

class PiggybankDto {
  final String id;
  final String name;
  const PiggybankDto({required this.id, required this.name});
  factory PiggybankDto.fromJson(Map<String, dynamic> json) => PiggybankDto(
    id: json['id'] as String,
    name: (json['attributes'] as Map<String, dynamic>)['name'] as String,
  );
}

class TagDto {
  final String id;
  final String tag;
  const TagDto({required this.id, required this.tag});
  factory TagDto.fromJson(Map<String, dynamic> json) => TagDto(
    id: json['id'] as String,
    tag: (json['attributes'] as Map<String, dynamic>)['tag'] as String,
  );
}
