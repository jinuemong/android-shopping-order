package woowacourse.shopping.domain.repository

import woowacourse.shopping.domain.model.CartItem
import woowacourse.shopping.domain.model.CartItemResult
import woowacourse.shopping.domain.model.Product
import woowacourse.shopping.domain.model.UpdateCartItemResult
import woowacourse.shopping.domain.model.UpdateCartItemType

interface ShoppingCartRepository {
    fun addCartItem(product: Product): Result<Unit>

    fun loadPagingCartItems(
        offset: Int,
        pagingSize: Int,
    ): Result<List<CartItem>>

    fun deleteCartItem(itemId: Long): Result<Unit>

    fun getCartItemResultFromProductId(productId: Long): Result<CartItemResult>

    fun updateCartItem(
        product: Product,
        updateCartItemType: UpdateCartItemType,
    ): Result<UpdateCartItemResult>

    fun getTotalCartItemCount(): Result<Int>
}
