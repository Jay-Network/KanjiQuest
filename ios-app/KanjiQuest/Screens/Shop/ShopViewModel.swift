import Foundation
import SharedCore

@MainActor
final class ShopViewModel: ObservableObject {
    @Published var catalog: [ShopItem] = []
    @Published var selectedCategory: ShopCategory? = nil
    @Published var balance: Int64 = 0
    @Published var ownedContentIds: Set<String> = []
    @Published var isLoading = true
    @Published var purchaseDialogItem: ShopItem? = nil
    @Published var purchaseResult: PurchaseResult? = nil
    @Published var featuredItem: ShopItem? = nil
    @Published var hasRedeemedTrial = false

    var categories: [ShopCategory] {
        Array(Set(catalog.map { $0.category }))
    }

    var filteredItems: [ShopItem] {
        guard let cat = selectedCategory else { return catalog }
        return catalog.filter { $0.category == cat }
    }

    private var jCoinRepository: JCoinRepository?
    private var userSessionProvider: UserSessionProviderImpl?

    func load(container: AppContainer) {
        jCoinRepository = container.jCoinRepository
        userSessionProvider = container.userSessionProvider

        Task {
            let userId = userSessionProvider?.getUserId() ?? ""

            // Load catalog
            catalog = (try? await jCoinRepository?.getShopCatalog()) ?? []

            // Load balance
            balance = (try? await jCoinRepository?.getBalance(userId: userId))?.displayBalance ?? 0

            // Load owned items
            var owned = Set<String>()
            for cat in ShopCategory.allCases {
                let unlocked = (try? await jCoinRepository?.getUnlockedContent(userId: userId, category: cat.name.lowercased())) ?? []
                owned.formUnion(unlocked)
            }
            ownedContentIds = owned

            featuredItem = catalog.first { $0.id == "tutoringjay_trial" }
            hasRedeemedTrial = featuredItem?.contentId != nil && ownedContentIds.contains(featuredItem?.contentId ?? "")

            isLoading = false
        }
    }

    func selectCategory(_ category: ShopCategory?) {
        selectedCategory = category
    }

    func showPurchaseDialog(_ item: ShopItem) {
        purchaseDialogItem = item
        purchaseResult = nil
    }

    func dismissPurchaseDialog() {
        purchaseDialogItem = nil
        purchaseResult = nil
    }

    func confirmPurchase(_ item: ShopItem) {
        Task {
            let userId = userSessionProvider?.getUserId() ?? ""
            let result = try? await jCoinRepository?.purchaseItem(userId: userId, item: item)
            purchaseResult = result

            if case .success = result {
                // Refresh owned
                var owned = Set<String>()
                for cat in ShopCategory.allCases {
                    let unlocked = (try? await jCoinRepository?.getUnlockedContent(userId: userId, category: cat.name.lowercased())) ?? []
                    owned.formUnion(unlocked)
                }
                ownedContentIds = owned
                hasRedeemedTrial = featuredItem?.contentId != nil && owned.contains(featuredItem?.contentId ?? "")
            }
        }
    }

    func purchaseFeatured() {
        guard let item = featuredItem else { return }
        showPurchaseDialog(item)
    }
}
