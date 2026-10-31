package com.example.data.model

import com.example.data.local.entity.MealPlanEntity

data class IndianVegetable(
    val englishName: String,
    val hindiName: String,
    val category: String, // Leafy, Cruciferous, Gourd, Root, Pods
    val caloriesPer100g: Int,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val preparationTip: String
)

object IndianVegetableLibrary {
    val vegetables = listOf(
        IndianVegetable("Spinach", "Palak", "Leafy", 23, 2.9f, 3.6f, "Steam lightly, saute with garlic and cumin. Minimal oil."),
        IndianVegetable("Carrot", "Gajar", "Root", 41, 0.9f, 9.6f, "Dice and stir fry with french beans or grate into salad."),
        IndianVegetable("French Beans", "Beans", "Pods", 31, 1.8f, 7.0f, "Chop finely, steam or stir fry with a pinch of mustard seeds."),
        IndianVegetable("Cabbage", "Patta Gobhi", "Cruciferous", 25, 1.3f, 5.8f, "Shred and quick-saute on high flame with green chilies and turmeric."),
        IndianVegetable("Cauliflower", "Phool Gobhi", "Cruciferous", 25, 1.9f, 5.0f, "Cut into florets, roast or stir fry with ginger and coriander."),
        IndianVegetable("Bottle Gourd", "Lauki / Doodhi", "Gourd", 14, 0.6f, 3.4f, "Very low calorie, chop into cubes, pressure cook or stir fry."),
        IndianVegetable("Bell Pepper", "Shimla Mirch", "Vegetable", 20, 0.9f, 4.6f, "Pairs perfectly with paneer or eggs in a dry stir fry."),
        IndianVegetable("Tomato", "Tamatar", "Vegetable", 18, 0.9f, 3.9f, "Adds rich base and lycopene to dal and vegetable curries."),
        IndianVegetable("Onion", "Pyaz", "Root", 40, 1.1f, 9.3f, "Base aromatic, saute with 1 tsp oil until translucent."),
        IndianVegetable("Green Peas", "Matar", "Pods", 81, 5.4f, 14.5f, "Adds natural protein and sweetness to mixed vegetable bowls."),
        IndianVegetable("Bitter Gourd", "Karela", "Gourd", 17, 1.0f, 3.7f, "Slice thin, shallow saute with turmeric; excellent for glucose control."),
        IndianVegetable("Cucumber", "Kheera", "Gourd", 15, 0.7f, 3.6f, "Eat raw with black salt and lemon as a zero-effort volume side.")
    )

    fun getVegetableNames(): List<String> = vegetables.map { "${it.englishName} (${it.hindiName})" }
}

enum class IndianFoundationLunchOption(
    val title: String,
    val description: String,
    val approxCalories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatsGrams: Int,
    val isNonVeg: Boolean = false
) {
    DAL_BOWL(
        title = "Dal Bowl + 200g Vegetables",
        description = "Thick home-cooked dal (toor/moong/masoor), 200g sauteed mixed seasonal vegetables (gajar, beans, palak) in 1 tsp mustard or olive oil.",
        approxCalories = 410,
        proteinGrams = 22,
        carbsGrams = 54,
        fatsGrams = 9
    ),
    EGG_SCRAMBLE(
        title = "Egg + Vegetable Scramble (Bhurji)",
        description = "3 whole farm eggs scrambled with 200g chopped vegetables (shimla mirch, pyaz, tamatar, palak) in 1 tsp oil with green chilies.",
        approxCalories = 420,
        proteinGrams = 24,
        carbsGrams = 14,
        fatsGrams = 26
    ),
    PANEER_STIR_FRY(
        title = "Paneer Stir Fry + 200g Vegetables",
        description = "120g fresh paneer cubed and pan-tossed with 200g mixed vegetables (shimla mirch, gobhi, beans) in 1 tsp oil.",
        approxCalories = 460,
        proteinGrams = 25,
        carbsGrams = 16,
        fatsGrams = 31
    ),
    CHICKEN_STIR_FRY(
        title = "Grilled Chicken Breast + 200g Vegetables",
        description = "150g skinless chicken breast marinated in curd, ginger-garlic & spices, grilled or pan-seared with 200g vegetables in 1 tsp oil.",
        approxCalories = 440,
        proteinGrams = 38,
        carbsGrams = 12,
        fatsGrams = 12,
        isNonVeg = true
    )
}

enum class IndianFoundationDinnerOption(
    val title: String,
    val description: String,
    val approxCalories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatsGrams: Int,
    val isNonVeg: Boolean = false
) {
    TWO_EGGS_VEG(
        title = "2 Boiled / Poached Eggs + 200g Steamed Vegetables",
        description = "2 whole eggs with generous portion of steamed cauliflower, beans, and spinach tossed with chaat masala.",
        approxCalories = 260,
        proteinGrams = 16,
        carbsGrams = 12,
        fatsGrams = 15
    ),
    PANEER_VEG(
        title = "100g Fresh Paneer + 200g Sautéed Vegetables",
        description = "100g paneer lightly pan-toasted with 200g mixed cabbage, capsicum, and tomato in minimal oil.",
        approxCalories = 340,
        proteinGrams = 20,
        carbsGrams = 14,
        fatsGrams = 22
    ),
    TIRED_DAY_WHEY(
        title = "Tired / Low-Effort Day: Whey Protein in Water",
        description = "1-2 scoops whey protein isolate or concentrate shaken in cold water. Zero cooking needed when exhausted.",
        approxCalories = 130,
        proteinGrams = 25,
        carbsGrams = 3,
        fatsGrams = 1
    ),
    GRILLED_CHICKEN_DINNER(
        title = "150g Grilled Chicken + Steamed Greens",
        description = "150g tender chicken breast with a bowl of steamed spinach or cucumber salad.",
        approxCalories = 310,
        proteinGrams = 36,
        carbsGrams = 6,
        fatsGrams = 8,
        isNonVeg = true
    )
}

enum class CarbPortionOption(
    val title: String,
    val approxCalories: Int,
    val carbsGrams: Int,
    val proteinGrams: Int
) {
    NONE("No Carbs (Low Carb Meal)", 0, 0, 0),
    RICE_100G("100g Cooked Steamed Rice", 130, 28, 3),
    ONE_ROTI("1 Whole Wheat Roti (Phulka)", 100, 20, 3)
}
