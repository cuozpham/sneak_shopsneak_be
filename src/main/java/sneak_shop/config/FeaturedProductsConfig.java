package sneak_shop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.featured")
public class FeaturedProductsConfig {
    private int totalDisplayed = 12;
    private int maxPinned = 6;
    private int maxPerCategory = 3;
    private int recentSalesWindowDays = 30;
    private int minReviewsForRating = 3;

    public int getTotalDisplayed() { return totalDisplayed; }
    public void setTotalDisplayed(int v) { this.totalDisplayed = v; }
    public int getMaxPinned() { return maxPinned; }
    public void setMaxPinned(int v) { this.maxPinned = v; }
    public int getMaxPerCategory() { return maxPerCategory; }
    public void setMaxPerCategory(int v) { this.maxPerCategory = v; }
    public int getRecentSalesWindowDays() { return recentSalesWindowDays; }
    public void setRecentSalesWindowDays(int v) { this.recentSalesWindowDays = v; }
    public int getMinReviewsForRating() { return minReviewsForRating; }
    public void setMinReviewsForRating(int v) { this.minReviewsForRating = v; }
}
