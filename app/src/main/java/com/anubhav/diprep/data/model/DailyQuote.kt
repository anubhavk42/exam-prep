package com.anubhav.diprep.data.model

data class DailyQuote(
    val quote: String,
    val author: String
)

object MotivationalQuotes {
    val QUOTES = listOf(
        DailyQuote("Success is the sum of small efforts, repeated day in and day out.", "Robert Collier"),
        DailyQuote("Consistency is not about perfection, it's about refusing to give up.", "Drug Inspector Aspirant"),
        DailyQuote("To succeed in your mission, you must have single-minded devotion to your goal.", "Dr. A.P.J. Abdul Kalam"),
        DailyQuote("The secret of getting ahead is getting started.", "Mark Twain"),
        DailyQuote("Your DI badge is being forged in every practice MCQ you solve today.", "Aspirant Wisdom"),
        DailyQuote("Discipline is choosing between what you want now and what you want most.", "Abraham Lincoln"),
        DailyQuote("Believe you can and you're halfway there.", "Theodore Roosevelt")
    )

    fun getQuoteForDay(dayOfYear: Int): DailyQuote {
        val index = (dayOfYear.coerceAtLeast(0)) % QUOTES.size
        return QUOTES[index]
    }
}
