package com.example.data.repository

import kotlin.random.Random

enum class QuoteCategory(val label: String) {
    ALL("All 100+"),
    MOTIVATION("Motivation"),
    LIFE("Life Wisdom"),
    CALM("Calm"),
    FOCUS("Focus")
}

data class DailyQuote(
    val text: String,
    val category: QuoteCategory
)

object MotivationProvider {
    val library: List<DailyQuote> = listOf(
        // Motivation (28)
        DailyQuote("Small consistent steps today build unstoppable momentum tomorrow.", QuoteCategory.MOTIVATION),
        DailyQuote("Start where you are, use what you have, finish what matters first.", QuoteCategory.MOTIVATION),
        DailyQuote("Your future self will thank you for the discipline you show right now.", QuoteCategory.MOTIVATION),
        DailyQuote("Progress beats perfection every single day of the year.", QuoteCategory.MOTIVATION),
        DailyQuote("Do not wait for motivation—let the first five minutes create it.", QuoteCategory.MOTIVATION),
        DailyQuote("Keep promises to yourself, especially on busy days.", QuoteCategory.MOTIVATION),
        DailyQuote("Momentum starts the moment you begin—take the first action now.", QuoteCategory.MOTIVATION),
        DailyQuote("Consistency is your quiet superpower—keep the chain alive today.", QuoteCategory.MOTIVATION),
        DailyQuote("Make today's effort something you will be proud to review tonight.", QuoteCategory.MOTIVATION),
        DailyQuote("Every small proof of work is a vote for the person you are becoming.", QuoteCategory.MOTIVATION),
        DailyQuote("Courage is simply starting before everything feels effortless.", QuoteCategory.MOTIVATION),
        DailyQuote("Win the hour in front of you and the rest of the day follows.", QuoteCategory.MOTIVATION),
        DailyQuote("Great days are built one deliberate hour at a time.", QuoteCategory.MOTIVATION),
        DailyQuote("Less hesitation, more execution; let results speak clearly.", QuoteCategory.MOTIVATION),
        DailyQuote("Turn intentions into visible milestones before the day closes.", QuoteCategory.MOTIVATION),
        DailyQuote("Show up with energy today even when the step looks small.", QuoteCategory.MOTIVATION),
        DailyQuote("Every attempt teaches your craft something hesitation never could.", QuoteCategory.MOTIVATION),
        DailyQuote("You are always one focused session away from a breakthrough.", QuoteCategory.MOTIVATION),
        DailyQuote("Action cures doubt faster than endless planning ever will.", QuoteCategory.MOTIVATION),
        DailyQuote("Build habits that carry you forward even on low-energy mornings.", QuoteCategory.MOTIVATION),
        DailyQuote("Respect the process today and the results will take care of themselves.", QuoteCategory.MOTIVATION),
        DailyQuote("What feels hard this week becomes your warm-up next month.", QuoteCategory.MOTIVATION),
        DailyQuote("Choose steady persistence over brief bursts of intensity.", QuoteCategory.MOTIVATION),
        DailyQuote("Finish strong on what you started so your mind stays clear.", QuoteCategory.MOTIVATION),
        DailyQuote("Hold high standards for your effort and patience for your timeline.", QuoteCategory.MOTIVATION),
        DailyQuote("Today is another 24-hour canvas—paint it with deliberate progress.", QuoteCategory.MOTIVATION),
        DailyQuote("Every completed check-in strengthens your daily rhythm.", QuoteCategory.MOTIVATION),
        DailyQuote("Keep moving forward; even quiet progress reshapes the whole week.", QuoteCategory.MOTIVATION),

        // Life Wisdom (28)
        DailyQuote("How you spend your ordinary mornings quietly shapes your entire life.", QuoteCategory.LIFE),
        DailyQuote("Gratitude turns what you already have into more than enough.", QuoteCategory.LIFE),
        DailyQuote("Kindness costs little, yet its echo travels much further than you see.", QuoteCategory.LIFE),
        DailyQuote("Listen deeply when others speak; presence is the rarest gift.", QuoteCategory.LIFE),
        DailyQuote("Do not compare your behind-the-scenes effort with someone else's highlight reel.", QuoteCategory.LIFE),
        DailyQuote("Protect your curiosity—it keeps every season of life vibrant.", QuoteCategory.LIFE),
        DailyQuote("Let go of yesterday's clutter so today has room to breathe.", QuoteCategory.LIFE),
        DailyQuote("True wealth is having time, health, and people you genuinely care about.", QuoteCategory.LIFE),
        DailyQuote("Speak truthfully with warmth, and you will never regret your words.", QuoteCategory.LIFE),
        DailyQuote("Learn something small every day; wisdom compounds silently over years.", QuoteCategory.LIFE),
        DailyQuote("Slow down enough to actually taste your coffee and notice the sky.", QuoteCategory.LIFE),
        DailyQuote("Your boundaries teach the world how to respect your peace.", QuoteCategory.LIFE),
        DailyQuote("Celebrate other people's wins—generosity multiplies joy.", QuoteCategory.LIFE),
        DailyQuote("Forgive your past mistakes quickly, but keep every lesson they paid for.", QuoteCategory.LIFE),
        DailyQuote("Never sacrifice long-term character for short-term convenience.", QuoteCategory.LIFE),
        DailyQuote("Stay humble enough to learn from anyone and brave enough to try.", QuoteCategory.LIFE),
        DailyQuote("The quality of your questions often decides the quality of your life.", QuoteCategory.LIFE),
        DailyQuote("Invest time in relationships that make you wiser, kinder, and steadier.", QuoteCategory.LIFE),
        DailyQuote("Change begins the day you take full ownership of your next choice.", QuoteCategory.LIFE),
        DailyQuote("Happiness grows where attention meets appreciation.", QuoteCategory.LIFE),
        DailyQuote("Live so that your daily calendar reflects what you truly value.", QuoteCategory.LIFE),
        DailyQuote("Keep your circle honest, your habits simple, and your heart open.", QuoteCategory.LIFE),
        DailyQuote("Not every storm comes to disrupt you; some come to clear your path.", QuoteCategory.LIFE),
        DailyQuote("Time is the one currency you spend only once—spend it intentionally.", QuoteCategory.LIFE),
        DailyQuote("Be gentle with people; almost everyone is carrying something unseen.", QuoteCategory.LIFE),
        DailyQuote("Growth rarely announces itself while happening—trust the quiet seasons.", QuoteCategory.LIFE),
        DailyQuote("Walk at your own honest pace; rushing does not change the horizon.", QuoteCategory.LIFE),
        DailyQuote("Stay grounded, stay adaptable, and celebrate every milestone reached.", QuoteCategory.LIFE),

        // Calm (28)
        DailyQuote("Unclench your jaw, drop your shoulders, and take one slow breath.", QuoteCategory.CALM),
        DailyQuote("You do not have to solve the entire month in the next ten minutes.", QuoteCategory.CALM),
        DailyQuote("Calm is a superpower—protect your nervous system before reacting.", QuoteCategory.CALM),
        DailyQuote("Breathe deep, stay steady, and turn intention into visible proof.", QuoteCategory.CALM),
        DailyQuote("Pause for thirty quiet seconds; clarity returns when noise fades.", QuoteCategory.CALM),
        DailyQuote("Let hurry go—steady hands finish faster than frantic ones.", QuoteCategory.CALM),
        DailyQuote("Drink a glass of water and give your eyes twenty seconds off screen.", QuoteCategory.CALM),
        DailyQuote("Peace is not the absence of work; it is moving through work without panic.", QuoteCategory.CALM),
        DailyQuote("Stillness for two minutes saves twenty minutes of scattered worry.", QuoteCategory.CALM),
        DailyQuote("Show up with calm purpose; steady effort compounds quietly.", QuoteCategory.CALM),
        DailyQuote("Release what you cannot control today and pour energy into what you can.", QuoteCategory.CALM),
        DailyQuote("A rested mind spots simple solutions an exhausted mind walks right past.", QuoteCategory.CALM),
        DailyQuote("Soften the pace for a moment; balance keeps endurance alive.", QuoteCategory.CALM),
        DailyQuote("Breathe in for four counts, hold for four, exhale slowly for six.", QuoteCategory.CALM),
        DailyQuote("Quiet confidence needs no rush and no applause.", QuoteCategory.CALM),
        DailyQuote("Clear your desk, clear one worry, and return to the present minute.", QuoteCategory.CALM),
        DailyQuote("Even a brief stretch resets posture, mood, and patience.", QuoteCategory.CALM),
        DailyQuote("Treat rest as part of high performance, never as a guilty afterthought.", QuoteCategory.CALM),
        DailyQuote("When the schedule gets loud, answer one calm priority at a time.", QuoteCategory.CALM),
        DailyQuote("Kindness toward yourself helps you stay resilient when plans shift.", QuoteCategory.CALM),
        DailyQuote("Notice five quiet details around you—anchor right back into now.", QuoteCategory.CALM),
        DailyQuote("A calm tone diffuses tension before it can steal your afternoon.", QuoteCategory.CALM),
        DailyQuote("Close unnecessary tabs in your browser and in your mind.", QuoteCategory.CALM),
        DailyQuote("Step outside for two minutes of fresh air whenever your focus stalls.", QuoteCategory.CALM),
        DailyQuote("You are allowed to move at a sustainable, human rhythm today.", QuoteCategory.CALM),
        DailyQuote("Calm focus turns overwhelming tasks into simple checkboxes.", QuoteCategory.CALM),
        DailyQuote("Guard your evening wind-down so tomorrow starts clear and bright.", QuoteCategory.CALM),
        DailyQuote("Nothing blooms all year—honor both effort and quiet recovery.", QuoteCategory.CALM),

        // Focus (28)
        DailyQuote("Clarity comes from action—focus on the very next meaningful step.", QuoteCategory.FOCUS),
        DailyQuote("Protect your focus; one completed task outweighs ten half-started plans.", QuoteCategory.FOCUS),
        DailyQuote("Energy flows where attention goes—aim both at what matters most.", QuoteCategory.FOCUS),
        DailyQuote("A clear note and an honest step forward beat an endless to-do list.", QuoteCategory.FOCUS),
        DailyQuote("Stay patient with big goals and urgent with daily execution.", QuoteCategory.FOCUS),
        DailyQuote("You do not need to finish everything at once, only start cleanly.", QuoteCategory.FOCUS),
        DailyQuote("Turn distractions down and commitment up for the next block.", QuoteCategory.FOCUS),
        DailyQuote("Honor your schedule—your time deserves your full presence.", QuoteCategory.FOCUS),
        DailyQuote("Reset, refocus, and give this next task your best attention.", QuoteCategory.FOCUS),
        DailyQuote("Discipline feels light when you know why the task matters.", QuoteCategory.FOCUS),
        DailyQuote("Pick one needle-mover right now and close everything unrelated.", QuoteCategory.FOCUS),
        DailyQuote("Multitasking splits progress; single-tasking finishes projects.", QuoteCategory.FOCUS),
        DailyQuote("Write down the next physical step—vague goals invite procrastination.", QuoteCategory.FOCUS),
        DailyQuote("Guard your first deep-work hour before checking messages.", QuoteCategory.FOCUS),
        DailyQuote("Attention is your scarcest resource—place it deliberately.", QuoteCategory.FOCUS),
        DailyQuote("Define what 'done' looks like before starting the timer.", QuoteCategory.FOCUS),
        DailyQuote("Say a polite no to minor distractions so your major work thrives.", QuoteCategory.FOCUS),
        DailyQuote("Twenty unbroken minutes of deep work beat two distracted hours.", QuoteCategory.FOCUS),
        DailyQuote("Keep your active checklist short enough to finish cleanly today.", QuoteCategory.FOCUS),
        DailyQuote("Put your phone face down for fifteen minutes and watch output soar.", QuoteCategory.FOCUS),
        DailyQuote("Batch tiny errands together so your creative blocks stay intact.", QuoteCategory.FOCUS),
        DailyQuote("Ask yourself: if this were the only task finished today, would it count?", QuoteCategory.FOCUS),
        DailyQuote("Measure your day by meaningful completions, not hours spent busy.", QuoteCategory.FOCUS),
        DailyQuote("Simplify the workspace around you to sharpen the thinking inside you.", QuoteCategory.FOCUS),
        DailyQuote("Start ugly, refine later—momentum rewards unfinished drafts.", QuoteCategory.FOCUS),
        DailyQuote("Follow one checklist step at a time until the milestone locks in.", QuoteCategory.FOCUS),
        DailyQuote("When stuck, shrink the step until you can start in thirty seconds.", QuoteCategory.FOCUS),
        DailyQuote("Deep concentration turns ordinary talent into remarkable craftsmanship.", QuoteCategory.FOCUS)
    )

    val quotes: List<String> = library.map { it.text }

    fun quotesForCategory(category: QuoteCategory): List<DailyQuote> {
        return if (category == QuoteCategory.ALL) library else library.filter { it.category == category }
    }

    fun countForCategory(category: QuoteCategory): Int = quotesForCategory(category).size

    fun randomQuoteEntry(
        category: QuoteCategory = QuoteCategory.ALL,
        exclude: String? = null,
        random: Random = Random.Default
    ): DailyQuote {
        val base = quotesForCategory(category).ifEmpty { library }
        val pool = if (exclude != null && base.size > 1) {
            base.filter { it.text != exclude }
        } else {
            base
        }
        return pool[random.nextInt(pool.size)]
    }

    fun randomQuote(exclude: String? = null, random: Random = Random.Default): String {
        return randomQuoteEntry(QuoteCategory.ALL, exclude, random).text
    }

    fun composeNotificationBody(
        motivationLine: String,
        noteText: String,
        fallbackBody: String
    ): String {
        val cleanQuote = motivationLine.trim()
        val cleanNote = noteText.trim()
        return buildString {
            if (cleanQuote.isNotEmpty()) {
                append("\"$cleanQuote\"")
            }
            if (cleanNote.isNotEmpty()) {
                if (isNotEmpty()) append("\n")
                append("Notes: ").append(cleanNote)
            } else if (isEmpty()) {
                append(fallbackBody)
            }
        }
    }
}
