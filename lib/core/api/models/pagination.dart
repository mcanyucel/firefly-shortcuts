class Pagination {
  final int total;
  final int currentPage;
  final int totalPages;

  const Pagination({
    required this.total,
    required this.currentPage,
    required this.totalPages,
  });

  factory Pagination.fromJson(Map<String, dynamic> json) => Pagination(
    total: json['total'] as int,
    currentPage: json['current_page'] as int,
    totalPages: json['total_pages'] as int,
  );

  bool get hasNextPage => currentPage < totalPages;
}
