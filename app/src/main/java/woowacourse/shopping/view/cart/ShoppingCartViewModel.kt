package woowacourse.shopping.view.cart

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import woowacourse.shopping.data.repository.ShoppingCartRepositoryImpl.Companion.DEFAULT_ITEM_SIZE
import woowacourse.shopping.data.repository.remote.RemoteShoppingCartRepositoryImpl.Companion.LOAD_SHOPPING_ITEM_OFFSET
import woowacourse.shopping.data.repository.remote.RemoteShoppingCartRepositoryImpl.Companion.LOAD_SHOPPING_ITEM_SIZE
import woowacourse.shopping.domain.model.CartItem
import woowacourse.shopping.domain.model.Product
import woowacourse.shopping.domain.model.UpdateCartItemResult
import woowacourse.shopping.domain.model.UpdateCartItemType
import woowacourse.shopping.domain.repository.ShoppingCartRepository
import woowacourse.shopping.utils.exception.OrderException
import woowacourse.shopping.utils.livedata.MutableSingleLiveData
import woowacourse.shopping.utils.livedata.SingleLiveData
import woowacourse.shopping.view.BaseViewModel
import woowacourse.shopping.view.cart.model.ShoppingCart
import woowacourse.shopping.view.cartcounter.OnClickCartItemCounter
import woowacourse.shopping.view.model.event.ErrorEvent
import woowacourse.shopping.view.model.event.LoadEvent

class ShoppingCartViewModel(
    private val shoppingCartRepository: ShoppingCartRepository,
) : BaseViewModel(), OnClickCartItemCounter {
    val shoppingCart = ShoppingCart()

    private val _shoppingCartEvent: MutableLiveData<ShoppingCartEvent> =
        MutableLiveData()
    val shoppingCartEvent: LiveData<ShoppingCartEvent> get() = _shoppingCartEvent

    private val _loadingEvent: MutableSingleLiveData<LoadEvent> =
        MutableSingleLiveData()
    val loadingEvent: SingleLiveData<LoadEvent> get() = _loadingEvent

    val checkedShoppingCart = ShoppingCart()

    private val _allCheck: MutableLiveData<Boolean> = MutableLiveData(false)
    val allCheck: LiveData<Boolean> get() = _allCheck
    private val _totalPrice: MutableLiveData<Int> = MutableLiveData(0)
    val totalPrice: LiveData<Int> get() = _totalPrice
    private val _totalCount: MutableLiveData<Int> = MutableLiveData(0)
    val totalCount: LiveData<Int> get() = _totalCount

    fun deleteShoppingCartItem(
        cartItemId: Long,
        product: Product,
    ) {
        try {
            shoppingCartRepository.deleteCartItem(cartItemId)
            shoppingCart.deleteProduct(cartItemId)
            _shoppingCartEvent.value =
                ShoppingCartEvent.UpdateProductEvent.DELETE(productId = product.id)
            deleteCheckedItem(CartItem(cartItemId, product))
        } catch (e: Exception) {
            handleException(e)
        }
    }

    fun loadPagingCartItemList() {
        _loadingEvent.setValue(LoadEvent.Loading)
//        Handler(Looper.getMainLooper()).postDelayed({
        try {
            val pagingData =
                shoppingCartRepository.loadPagingCartItems(
                    LOAD_SHOPPING_ITEM_OFFSET,
                    LOAD_SHOPPING_ITEM_SIZE,
                )
            _loadingEvent.setValue(LoadEvent.Success)
            shoppingCart.addProducts(synchronizeLoadingData(pagingData))
            setAllCheck()
        } catch (e: Exception) {
            handleException(e)
        }
//        }, 1000)
    }

    private fun setAllCheck() {
        _allCheck.value = shoppingCart.cartItems.value?.all { it.cartItemSelector.isSelected }
    }

    fun checkAllItems() {
        if (allCheck.value == true) {
            shoppingCart.cartItems.value?.forEach { cartItem ->
                if (cartItem.cartItemSelector.isSelected) {
                    deleteCheckedItem(cartItem)
                }
            }
        } else {
            shoppingCart.cartItems.value?.forEach { cartItem ->
                if (!cartItem.cartItemSelector.isSelected) {
                    addCheckedItem(cartItem)
                }
            }
        }
        updateCheckItemData()
        _shoppingCartEvent.value = ShoppingCartEvent.UpdateCheckItem.Success
    }

    private fun synchronizeLoadingData(pagingData: List<CartItem>): List<CartItem> {
        return if ((totalCount.value ?: DEFAULT_ITEM_SIZE) > DEFAULT_ITEM_SIZE) {
            return pagingData.map { cartItem ->
                val checkedCartItem =
                    checkedShoppingCart.cartItems.value?.find { it.id == cartItem.id }
                if (checkedCartItem != null) {
                    if (checkedCartItem.cartItemSelector.isSelected) {
                        cartItem.cartItemSelector.selectItem()
                    } else {
                        cartItem.cartItemSelector.unSelectItem()
                    }
                }
                cartItem
            }
        } else {
            pagingData
        }
    }

    private fun updateCartItem(
        product: Product,
        updateCartItemType: UpdateCartItemType,
    ) {
        try {
            val updateCartItemResult =
                shoppingCartRepository.updateCartItem(
                    product,
                    updateCartItemType,
                )
            when (updateCartItemResult) {
                UpdateCartItemResult.ADD -> throw OrderException()
                is UpdateCartItemResult.DELETE ->
                    deleteShoppingCartItem(
                        updateCartItemResult.cartItemId,
                        product = product,
                    )

                is UpdateCartItemResult.UPDATED -> {
                    product.updateCartItemCount(updateCartItemResult.cartItemResult.counter.itemCount)
                    _shoppingCartEvent.value =
                        ShoppingCartEvent.UpdateProductEvent.Success(
                            productId = product.id,
                            count = product.cartItemCounter.itemCount,
                        )
                }
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    fun addCheckedItem(cartItem: CartItem) {
        cartItem.cartItemSelector.selectItem()
        checkedShoppingCart.addProduct(cartItem)
        checkedShoppingCart
        updateCheckItemData()
    }

    fun deleteCheckedItem(cartItem: CartItem) {
        cartItem.cartItemSelector.unSelectItem()
        checkedShoppingCart.deleteProduct(cartItem.id)
        updateCheckItemData()
    }

    private fun updateCheckItemData() {
        _totalPrice.value = checkedShoppingCart.cartItems.value?.sumOf {
            it.product.cartItemCounter.itemCount * it.product.price
        } ?: DEFAULT_ITEM_SIZE
        _totalCount.value = checkedShoppingCart.cartItems.value?.count {
            it.cartItemSelector.isSelected
        } ?: DEFAULT_ITEM_SIZE
        setAllCheck()
    }

    override fun clickIncrease(product: Product) {
        updateCartItem(product, UpdateCartItemType.INCREASE)
    }

    override fun clickDecrease(product: Product) {
        updateCartItem(product, UpdateCartItemType.DECREASE)
    }
}
