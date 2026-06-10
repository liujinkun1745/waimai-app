package com.waimai.service;

import com.waimai.entity.Category;
import com.waimai.entity.Product;
import com.waimai.exception.BusinessException;
import com.waimai.repository.CategoryRepository;
import com.waimai.repository.MerchantRepository;
import com.waimai.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final MerchantRepository merchantRepository;
    private final CategoryRepository categoryRepository;

    public List<Product> listByCategory(Long merchantId, Long categoryId) {
        return productRepository.findByMerchantIdAndCategoryIdAndStatus(merchantId, categoryId, "上架");
    }

    public List<Product> listOnSale(Long merchantId) {
        return productRepository.findByMerchantIdAndStatus(merchantId, "上架");
    }

    public List<Product> listAll(Long merchantId) {
        return productRepository.findByMerchantIdWithCategory(merchantId);
    }

    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("商品不存在"));
    }

    @Transactional
    public Product add(Long merchantId, Long categoryId, String name,
                        BigDecimal price, Integer stock, String image, String description) {
        Product product = Product.builder()
                .merchant(merchantRepository.getReferenceById(merchantId))
                .category(categoryRepository.getReferenceById(categoryId))
                .name(name)
                .price(price)
                .stock(stock)
                .image(image)
                .description(description)
                .status("上架")
                .build();
        return productRepository.save(product);
    }

    @Transactional
    public void update(Long productId, Long categoryId, String name,
                        BigDecimal price, Integer stock, String image, String description) {
        Product product = findById(productId);
        product.setCategory(categoryRepository.getReferenceById(categoryId));
        product.setName(name);
        product.setPrice(price);
        product.setStock(stock);
        if (image != null && !image.isBlank()) {
            product.setImage(image);
        }
        product.setDescription(description);
        productRepository.save(product);
    }

    @Transactional
    public void toggleStatus(Long productId) {
        Product product = findById(productId);
        product.setStatus("上架".equals(product.getStatus()) ? "下架" : "上架");
        productRepository.save(product);
    }

    @Transactional
    public void delete(Long productId) {
        productRepository.deleteById(productId);
    }

    /** 移动商品到另一个分类 */
    @Transactional
    public void moveCategory(Long productId, Long categoryId) {
        Product product = findById(productId);
        product.setCategory(categoryRepository.getReferenceById(categoryId));
        productRepository.save(product);
    }
}
