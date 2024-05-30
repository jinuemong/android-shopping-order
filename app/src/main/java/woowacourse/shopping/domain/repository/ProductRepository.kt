package woowacourse.shopping.domain.repository

import woowacourse.shopping.domain.model.Product

interface ProductRepository {
    fun loadPagingProducts(offset: Int): List<Product>

    fun loadCategoryProducts(
        size: Int,
        category: String,
    ): List<Product>

    fun getProduct(productId: Long): Product
}
