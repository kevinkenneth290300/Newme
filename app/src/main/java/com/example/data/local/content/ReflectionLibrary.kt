package com.example.data.local.content

data class ReflectionItem(
    val id: String,
    val timeOfDay: String, // MORNING, MIDDAY, EVENING
    val principle: String,
    val category: String,
    val moods: List<String>,
    val text: String,
    val authorOrInspiration: String = "Stoic Principle"
)

object ReflectionLibrary {

    val reflections: List<ReflectionItem> = listOf(
        // ================= MORNING REFLECTIONS (35 Items) =================
        ReflectionItem(
            id = "m_control_01",
            timeOfDay = "MORNING",
            principle = "Dichotomy of Control",
            category = "Control",
            moods = listOf("Energized", "Good", "Okay", "Stressed"),
            text = "You do not control the noise of the day or other people's choices. You control your attention, your effort, and how you respond right now."
        ),
        ReflectionItem(
            id = "m_control_02",
            timeOfDay = "MORNING",
            principle = "Dichotomy of Control",
            category = "Focus",
            moods = listOf("Stressed", "Tired", "Okay"),
            text = "Separate what is up to you from what is not. Pour your strength only into the territory you actually govern."
        ),
        ReflectionItem(
            id = "m_discipline_01",
            timeOfDay = "MORNING",
            principle = "Discipline",
            category = "Action",
            moods = listOf("Unmotivated", "Tired", "Okay"),
            text = "Do not wait to feel like doing the work. Action produces clarity; waiting produces hesitation. Move first."
        ),
        ReflectionItem(
            id = "m_discipline_02",
            timeOfDay = "MORNING",
            principle = "Discipline",
            category = "Self-Respect",
            moods = listOf("Energized", "Good", "Okay"),
            text = "Discipline is simply keeping a promise you made to yourself after the mood you made it in has passed."
        ),
        ReflectionItem(
            id = "m_discipline_03",
            timeOfDay = "MORNING",
            principle = "Discipline",
            category = "Action",
            moods = listOf("Unmotivated", "Tired"),
            text = "Small standards upheld without compromise build an unshakable character. Win the first physical hour."
        ),
        ReflectionItem(
            id = "m_courage_01",
            timeOfDay = "MORNING",
            principle = "Courage",
            category = "Preparation",
            moods = listOf("Stressed", "Okay", "Unmotivated"),
            text = "Approach the hardest task on your schedule early. Fear shrinks the moment you turn to face it directly."
        ),
        ReflectionItem(
            id = "m_patience_01",
            timeOfDay = "MORNING",
            principle = "Patience",
            category = "Virtue",
            moods = listOf("Energized", "Good", "Okay"),
            text = "A great life is not built in a single heroic morning. It is assembled block by quiet block over ordinary days."
        ),
        ReflectionItem(
            id = "m_memento_01",
            timeOfDay = "MORNING",
            principle = "Memento Mori",
            category = "Focus",
            moods = listOf("Good", "Energized", "Okay", "Stressed"),
            text = "You may not have tomorrow. Treat this morning as an unrepeatable privilege, not a rehearsal."
        ),
        ReflectionItem(
            id = "m_virtue_01",
            timeOfDay = "MORNING",
            principle = "Virtue",
            category = "Self-Respect",
            moods = listOf("Good", "Energized"),
            text = "Let your actions be deliberate and clean today. Integrity does not depend on whether anyone is watching."
        ),
        ReflectionItem(
            id = "m_acceptance_01",
            timeOfDay = "MORNING",
            principle = "Acceptance",
            category = "Calm",
            moods = listOf("Stressed", "Tired"),
            text = "Welcome the day as it is, including its frictions. Friction is where strength is forged."
        ),
        ReflectionItem(
            id = "m_gratitude_01",
            timeOfDay = "MORNING",
            principle = "Gratitude",
            category = "Gratitude",
            moods = listOf("Good", "Energized", "Okay"),
            text = "Before you demand more from the world today, notice what you already possess: breath, reason, and another chance to act."
        ),
        ReflectionItem(
            id = "m_action_01",
            timeOfDay = "MORNING",
            principle = "Action",
            category = "Focus",
            moods = listOf("Unmotivated", "Okay"),
            text = "Cut away the non-essential. Most of what drains our energy today is work we never needed to agree to."
        ),
        ReflectionItem(
            id = "m_responsibility_01",
            timeOfDay = "MORNING",
            principle = "Responsibility",
            category = "Self-Respect",
            moods = listOf("Energized", "Good", "Stressed"),
            text = "Blaming circumstances changes nothing. Claim full ownership over your diet, your training, and your temper today."
        ),
        ReflectionItem(
            id = "m_premed_01",
            timeOfDay = "MORNING",
            principle = "Premeditation of Difficulties",
            category = "Preparation",
            moods = listOf("Stressed", "Okay"),
            text = "Expect distractions and uncooperative situations today. When they arrive, treat them as the training ground you prepared for."
        ),
        ReflectionItem(
            id = "m_focus_01",
            timeOfDay = "MORNING",
            principle = "Focus",
            category = "Focus",
            moods = listOf("Energized", "Good", "Okay"),
            text = "Do every deed as if it were the last of your life, free from restlessness and half-hearted effort."
        ),
        ReflectionItem(
            id = "m_service_01",
            timeOfDay = "MORNING",
            principle = "Service",
            category = "Virtue",
            moods = listOf("Good", "Energized"),
            text = "What hurts the hive hurts the bee. Bring calm and useful energy to everyone you interact with today."
        ),
        ReflectionItem(
            id = "m_recovery_01",
            timeOfDay = "MORNING",
            principle = "Recovery",
            category = "Recovery",
            moods = listOf("Tired", "Unmotivated"),
            text = "Yesterday's shortcomings are dead. Today is a clean slate. Begin with one honest, disciplined choice right now."
        ),
        ReflectionItem(
            id = "m_control_03",
            timeOfDay = "MORNING",
            principle = "Dichotomy of Control",
            category = "Calm",
            moods = listOf("Stressed", "Tired"),
            text = "You are not required to have an opinion on everything that happens. Guard your mental peace as your primary asset."
        ),
        ReflectionItem(
            id = "m_discipline_04",
            timeOfDay = "MORNING",
            principle = "Discipline",
            category = "Action",
            moods = listOf("Unmotivated", "Okay"),
            text = "How long will you wait before demanding the best for yourself? Let your standards begin with breakfast and your morning water."
        ),
        ReflectionItem(
            id = "m_patience_02",
            timeOfDay = "MORNING",
            principle = "Patience",
            category = "Virtue",
            moods = listOf("Good", "Energized"),
            text = "Nature never rushes, yet everything is accomplished. Commit to steady pacing rather than frantic bursts."
        ),
        ReflectionItem(
            id = "m_virtue_02",
            timeOfDay = "MORNING",
            principle = "Virtue",
            category = "Self-Respect",
            moods = listOf("Good", "Okay"),
            text = "Ask yourself before every action: is this necessary, or am I running on auto-pilot? Live deliberately."
        ),
        ReflectionItem(
            id = "m_courage_02",
            timeOfDay = "MORNING",
            principle = "Courage",
            category = "Preparation",
            moods = listOf("Stressed", "Tired"),
            text = "Difficulty shows what individuals are made of. When trouble comes, remember that the day has paired you with a rough sparring partner."
        ),
        ReflectionItem(
            id = "m_acceptance_02",
            timeOfDay = "MORNING",
            principle = "Acceptance",
            category = "Calm",
            moods = listOf("Stressed", "Okay"),
            text = "Accept whatever arrives today as fuel for your fire. Wood turns fire to ash; strong fire turns all wood to flame."
        ),
        ReflectionItem(
            id = "m_action_02",
            timeOfDay = "MORNING",
            principle = "Action",
            category = "Action",
            moods = listOf("Unmotivated", "Tired"),
            text = "No more arguing about what a disciplined person should be. Be one today in your simplest physical deeds."
        ),
        ReflectionItem(
            id = "m_memento_02",
            timeOfDay = "MORNING",
            principle = "Memento Mori",
            category = "Focus",
            moods = listOf("Energized", "Good"),
            text = "Time is the only asset that cannot be refunded. Spend today's hours on what truly sustains your future self."
        ),
        ReflectionItem(
            id = "m_gratitude_02",
            timeOfDay = "MORNING",
            principle = "Gratitude",
            category = "Gratitude",
            moods = listOf("Good", "Okay"),
            text = "He is a wise person who does not grieve for the things he has not, but rejoices for those he has."
        ),
        ReflectionItem(
            id = "m_responsibility_02",
            timeOfDay = "MORNING",
            principle = "Responsibility",
            category = "Self-Respect",
            moods = listOf("Energized", "Good"),
            text = "Never depend on motivation. Motivation is a fair-weather friend. Build your day on routines that run regardless of mood."
        ),
        ReflectionItem(
            id = "m_focus_02",
            timeOfDay = "MORNING",
            principle = "Focus",
            category = "Focus",
            moods = listOf("Stressed", "Okay"),
            text = "If you want peace, do less. Or rather, do only what is essential and do it thoroughly."
        ),
        ReflectionItem(
            id = "m_control_04",
            timeOfDay = "MORNING",
            principle = "Dichotomy of Control",
            category = "Control",
            moods = listOf("Stressed", "Tired"),
            text = "Do not seek for things to happen the way you want them to; rather, wish that what happens happens the way it happens."
        ),
        ReflectionItem(
            id = "m_discipline_05",
            timeOfDay = "MORNING",
            principle = "Discipline",
            category = "Action",
            moods = listOf("Unmotivated", "Okay"),
            text = "Discipline over mood. When you train your body and respect your nutrition, your mind naturally follows."
        ),
        ReflectionItem(
            id = "m_patience_03",
            timeOfDay = "MORNING",
            principle = "Patience",
            category = "Virtue",
            moods = listOf("Good", "Okay"),
            text = "Nothing important is created suddenly, any more than a bunch of grapes or a fig. Give your habits time to ripen."
        ),
        ReflectionItem(
            id = "m_service_02",
            timeOfDay = "MORNING",
            principle = "Service",
            category = "Virtue",
            moods = listOf("Energized", "Good"),
            text = "We are made for cooperation, like feet, like hands, like the rows of the upper and lower teeth."
        ),
        ReflectionItem(
            id = "m_courage_03",
            timeOfDay = "MORNING",
            principle = "Courage",
            category = "Preparation",
            moods = listOf("Stressed", "Unmotivated"),
            text = "First say to yourself what you would be; and then do what you have to do."
        ),
        ReflectionItem(
            id = "m_recovery_02",
            timeOfDay = "MORNING",
            principle = "Recovery",
            category = "Recovery",
            moods = listOf("Tired", "Unmotivated"),
            text = "A stumble is not a fall. Pick yourself up, dust off your knees, and execute today's simple protocol."
        ),
        ReflectionItem(
            id = "m_focus_03",
            timeOfDay = "MORNING",
            principle = "Focus",
            category = "Focus",
            moods = listOf("Energized", "Good", "Okay"),
            text = "Concentrate every minute like a Roman—on doing what’s in front of you with precise and genuine seriousness."
        ),

        // ================= MIDDAY REFLECTIONS (35 Items) =================
        ReflectionItem(
            id = "d_fatigue_01",
            timeOfDay = "MIDDAY",
            principle = "Discipline",
            category = "Fatigue",
            moods = listOf("Tired", "Behind", "Busy"),
            text = "Energy dips in the afternoon are physiological, not moral failures. Drink water, take five deep breaths, and reset your posture."
        ),
        ReflectionItem(
            id = "d_stress_01",
            timeOfDay = "MIDDAY",
            principle = "Dichotomy of Control",
            category = "Stress",
            moods = listOf("Stressed", "Busy"),
            text = "When deadlines pile up, slow down your internal speed. Rushing creates mistakes that cost double the time."
        ),
        ReflectionItem(
            id = "d_distract_01",
            timeOfDay = "MIDDAY",
            principle = "Focus",
            category = "Distraction",
            moods = listOf("Tired", "Behind", "Okay"),
            text = "Your attention is being auctioned off every minute. Reclaim it. Put the phone down and finish the task before you."
        ),
        ReflectionItem(
            id = "d_momentum_01",
            timeOfDay = "MIDDAY",
            principle = "Action",
            category = "Momentum",
            moods = listOf("Going well", "Great"),
            text = "You have built momentum this morning. Protect it by making a clean, balanced choice for lunch."
        ),
        ReflectionItem(
            id = "d_behind_01",
            timeOfDay = "MIDDAY",
            principle = "Recovery",
            category = "Falling behind",
            moods = listOf("Behind", "Stressed", "Tired"),
            text = "If the morning got away from you, do not write off the afternoon. The second half of the day is still waiting to be won."
        ),
        ReflectionItem(
            id = "d_tempt_01",
            timeOfDay = "MIDDAY",
            principle = "Discipline",
            category = "Temptation",
            moods = listOf("Tired", "Stressed"),
            text = "Sugar and impulsive snacks promise quick comfort but guarantee brain fog. Choose the food that respects your 5 PM self."
        ),
        ReflectionItem(
            id = "d_frustrate_01",
            timeOfDay = "MIDDAY",
            principle = "Patience",
            category = "Frustration",
            moods = listOf("Stressed", "Behind"),
            text = "Whenever someone tests your patience today, remind yourself: they act from their perspective, just as you act from yours."
        ),
        ReflectionItem(
            id = "d_action_01",
            timeOfDay = "MIDDAY",
            principle = "Action",
            category = "Focus",
            moods = listOf("Busy", "Going well"),
            text = "Do not try to solve the entire week this afternoon. Execute just the next single commitment on your plan."
        ),
        ReflectionItem(
            id = "d_control_01",
            timeOfDay = "MIDDAY",
            principle = "Dichotomy of Control",
            category = "Control",
            moods = listOf("Stressed", "Busy"),
            text = "Circumstances don't define you; how you bear them does. Remain steady amidst the midday rush."
        ),
        ReflectionItem(
            id = "d_recovery_01",
            timeOfDay = "MIDDAY",
            principle = "Recovery",
            category = "Recovery",
            moods = listOf("Behind", "Tired"),
            text = "A bad hour is not a bad day. Reset your mind right here, right now. Step forward cleanly."
        ),
        ReflectionItem(
            id = "d_patience_01",
            timeOfDay = "MIDDAY",
            principle = "Patience",
            category = "Virtue",
            moods = listOf("Busy", "Stressed"),
            text = "Patience is not passive waiting; it is active endurance with dignity. Keep your composure."
        ),
        ReflectionItem(
            id = "d_virtue_01",
            timeOfDay = "MIDDAY",
            principle = "Virtue",
            category = "Self-Respect",
            moods = listOf("Great", "Going well"),
            text = "Keep your promises when it is inconvenient. That is where real discipline separates from casual enthusiasm."
        ),
        ReflectionItem(
            id = "d_courage_01",
            timeOfDay = "MIDDAY",
            principle = "Courage",
            category = "Action",
            moods = listOf("Behind", "Stressed"),
            text = "Are you avoiding a conversation or an uncomfortable task? Meet it directly this afternoon and free your mind."
        ),
        ReflectionItem(
            id = "d_focus_01",
            timeOfDay = "MIDDAY",
            principle = "Focus",
            category = "Focus",
            moods = listOf("Busy", "Okay"),
            text = "Multitasking is an illusion that produces exhaustion. Complete one thing, clear your desk, then begin the next."
        ),
        ReflectionItem(
            id = "d_gratitude_01",
            timeOfDay = "MIDDAY",
            principle = "Gratitude",
            category = "Gratitude",
            moods = listOf("Great", "Going well", "Okay"),
            text = "Take thirty seconds to appreciate the strength your body has given you so far today. Hydrate and replenish."
        ),
        ReflectionItem(
            id = "d_memento_01",
            timeOfDay = "MIDDAY",
            principle = "Memento Mori",
            category = "Focus",
            moods = listOf("Stressed", "Busy"),
            text = "In a year, today's petty annoyances will be completely forgotten. Do not let them steal your peace this afternoon."
        ),
        ReflectionItem(
            id = "d_fatigue_02",
            timeOfDay = "MIDDAY",
            principle = "Recovery",
            category = "Fatigue",
            moods = listOf("Tired", "Behind"),
            text = "When physical energy drops, lean on your structure. Routines exist precisely for when motivation disappears."
        ),
        ReflectionItem(
            id = "d_distract_02",
            timeOfDay = "MIDDAY",
            principle = "Focus",
            category = "Distraction",
            moods = listOf("Tired", "Okay"),
            text = "The urge to escape into scrolling is just restlessness. Sit with the boredom for two minutes; focus will return."
        ),
        ReflectionItem(
            id = "d_control_02",
            timeOfDay = "MIDDAY",
            principle = "Dichotomy of Control",
            category = "Control",
            moods = listOf("Stressed", "Behind"),
            text = "You cannot force outcomes, but you can always control effort and attitude. Put effort into the remaining hours."
        ),
        ReflectionItem(
            id = "d_tempt_02",
            timeOfDay = "MIDDAY",
            principle = "Discipline",
            category = "Temptation",
            moods = listOf("Tired", "Stressed"),
            text = "Discipline is choosing between what you want now and what you want most. Protect your long-term goals."
        ),
        ReflectionItem(
            id = "d_action_02",
            timeOfDay = "MIDDAY",
            principle = "Action",
            category = "Momentum",
            moods = listOf("Going well", "Busy"),
            text = "Half the day has passed. What is the single most valuable action you can take before sunset? Focus on that."
        ),
        ReflectionItem(
            id = "d_frustrate_02",
            timeOfDay = "MIDDAY",
            principle = "Acceptance",
            category = "Frustration",
            moods = listOf("Stressed", "Behind"),
            text = "When things don't go according to plan, the plan becomes: handle things as they are. Adapt smoothly."
        ),
        ReflectionItem(
            id = "d_service_01",
            timeOfDay = "MIDDAY",
            principle = "Service",
            category = "Virtue",
            moods = listOf("Great", "Going well"),
            text = "Be strict with yourself, but generous and tolerant with others. Do not expect perfection from the world."
        ),
        ReflectionItem(
            id = "d_responsibility_01",
            timeOfDay = "MIDDAY",
            principle = "Responsibility",
            category = "Self-Respect",
            moods = listOf("Behind", "Busy"),
            text = "Excuses will always be available; opportunities to practice character will not. Own this afternoon."
        ),
        ReflectionItem(
            id = "d_momentum_02",
            timeOfDay = "MIDDAY",
            principle = "Action",
            category = "Momentum",
            moods = listOf("Going well", "Great"),
            text = "Consistency compounds quietly. Keep stacking honest actions until your daily commitments are secure."
        ),
        ReflectionItem(
            id = "d_behind_02",
            timeOfDay = "MIDDAY",
            principle = "Recovery",
            category = "Falling behind",
            moods = listOf("Behind", "Stressed"),
            text = "Do not lament lost hours. Start where you stand right now with what you have."
        ),
        ReflectionItem(
            id = "d_courage_02",
            timeOfDay = "MIDDAY",
            principle = "Courage",
            category = "Action",
            moods = listOf("Tired", "Behind"),
            text = "Courage is often not a shout, but the quiet voice at midday saying, 'I will try again.'"
        ),
        ReflectionItem(
            id = "d_virtue_02",
            timeOfDay = "MIDDAY",
            principle = "Virtue",
            category = "Self-Respect",
            moods = listOf("Going well", "Okay"),
            text = "Never esteem anything as of advantage to you that will make you break your word or lose your self-respect."
        ),
        ReflectionItem(
            id = "d_patience_02",
            timeOfDay = "MIDDAY",
            principle = "Patience",
            category = "Virtue",
            moods = listOf("Busy", "Stressed"),
            text = "Slow is smooth, and smooth is fast. Breathe out the rush and execute with poise."
        ),
        ReflectionItem(
            id = "d_control_03",
            timeOfDay = "MIDDAY",
            principle = "Dichotomy of Control",
            category = "Control",
            moods = listOf("Behind", "Tired"),
            text = "You don't need things to be easy; you only need them to be possible. Keep moving forward."
        ),
        ReflectionItem(
            id = "d_fatigue_03",
            timeOfDay = "MIDDAY",
            principle = "Recovery",
            category = "Fatigue",
            moods = listOf("Tired", "Behind"),
            text = "Hydrate now. Physical sluggishness is frequently nothing more than mild dehydration and poor oxygenation."
        ),
        ReflectionItem(
            id = "d_focus_02",
            timeOfDay = "MIDDAY",
            principle = "Focus",
            category = "Focus",
            moods = listOf("Okay", "Busy"),
            text = "Do not let the future disturb you. You will face it, if you have to, with the same weapons of reason."
        ),
        ReflectionItem(
            id = "d_tempt_03",
            timeOfDay = "MIDDAY",
            principle = "Discipline",
            category = "Temptation",
            moods = listOf("Tired", "Okay"),
            text = "Every time you choose water over soda or whole food over junk, your inner discipline strengthens."
        ),
        ReflectionItem(
            id = "d_recovery_02",
            timeOfDay = "MIDDAY",
            principle = "Recovery",
            category = "Recovery",
            moods = listOf("Behind", "Tired"),
            text = "Winning the afternoon is the best cure for a messy morning. Step into it with renewed resolve."
        ),
        ReflectionItem(
            id = "d_action_03",
            timeOfDay = "MIDDAY",
            principle = "Action",
            category = "Action",
            moods = listOf("Going well", "Great"),
            text = "Finish strong. Carry the discipline of your early day right into the close of work."
        ),

        // ================= EVENING REFLECTIONS (30 Items) =================
        ReflectionItem(
            id = "e_complete_01",
            timeOfDay = "EVENING",
            principle = "Reflection",
            category = "Completion",
            moods = listOf("Great", "Going well"),
            text = "Review today without vanity or self-punishment. Where did you keep your word? What can you sharpen tomorrow?"
        ),
        ReflectionItem(
            id = "e_forgive_01",
            timeOfDay = "EVENING",
            principle = "Acceptance",
            category = "Forgiveness",
            moods = listOf("Behind", "Tired", "Stressed"),
            text = "Today wasn't perfect, but you returned and faced it. Do not carry guilt into the night. Close the book cleanly."
        ),
        ReflectionItem(
            id = "e_rest_01",
            timeOfDay = "EVENING",
            principle = "Rest",
            category = "Rest",
            moods = listOf("Tired", "Okay"),
            text = "Sleep is not lost time; it is active recovery for your brain and muscle tissue. Protect your sleep window tonight."
        ),
        ReflectionItem(
            id = "e_env_01",
            timeOfDay = "EVENING",
            principle = "Discipline",
            category = "Environment",
            moods = listOf("Okay", "Going well"),
            text = "Reset your environment before bed. A clean sink, an orderly desk, and laid-out clothes invite a peaceful morning."
        ),
        ReflectionItem(
            id = "e_tomorrow_01",
            timeOfDay = "EVENING",
            principle = "Preparation",
            category = "Tomorrow",
            moods = listOf("Good", "Okay"),
            text = "Decide your top 3 non-negotiables for tomorrow before your head hits the pillow. Clear decisions create restful sleep."
        ),
        ReflectionItem(
            id = "e_gratitude_01",
            timeOfDay = "EVENING",
            principle = "Gratitude",
            category = "Gratitude",
            moods = listOf("Good", "Great", "Okay"),
            text = "Whatever happened today, you made it through. Give thanks for shelter, nourishment, and a safe bed."
        ),
        ReflectionItem(
            id = "e_control_01",
            timeOfDay = "EVENING",
            principle = "Dichotomy of Control",
            category = "Control",
            moods = listOf("Stressed", "Tired"),
            text = "Leave work at work. You cannot solve tomorrow's challenges tonight; you can only degrade tonight's rest."
        ),
        ReflectionItem(
            id = "e_virtue_01",
            timeOfDay = "EVENING",
            principle = "Virtue",
            category = "Self-Respect",
            moods = listOf("Great", "Good"),
            text = "Quiet self-respect is the sweetest pillow. If you acted with honor and effort today, you have won."
        ),
        ReflectionItem(
            id = "e_memento_01",
            timeOfDay = "EVENING",
            principle = "Memento Mori",
            category = "Reflection",
            moods = listOf("Okay", "Good"),
            text = "As you lie down tonight, reflect that sleep is a small rehearsal for the final rest. Cherish the life within you."
        ),
        ReflectionItem(
            id = "e_patience_01",
            timeOfDay = "EVENING",
            principle = "Patience",
            category = "Patience",
            moods = listOf("Behind", "Tired"),
            text = "True transformation is invisible on a single day, but undeniable across a hundred. Trust the compounding process."
        ),
        ReflectionItem(
            id = "e_acceptance_01",
            timeOfDay = "EVENING",
            principle = "Acceptance",
            category = "Acceptance",
            moods = listOf("Stressed", "Behind"),
            text = "What is done is done. Untie the knots of regret. Your energy is needed for tomorrow's sunrise."
        ),
        ReflectionItem(
            id = "e_recovery_01",
            timeOfDay = "EVENING",
            principle = "Recovery",
            category = "Recovery",
            moods = listOf("Tired", "Behind"),
            text = "Never miss two days in a row. Tomorrow you return to the arena. Rest tonight with full permission to recover."
        ),
        ReflectionItem(
            id = "e_env_02",
            timeOfDay = "EVENING",
            principle = "Discipline",
            category = "Environment",
            moods = listOf("Tired", "Okay"),
            text = "Ten minutes spent tidying your living space tonight will pay compound interest in calm tomorrow morning."
        ),
        ReflectionItem(
            id = "e_rest_02",
            timeOfDay = "EVENING",
            principle = "Rest",
            category = "Rest",
            moods = listOf("Tired", "Stressed"),
            text = "Dim the screens and lower the room temperature. Prepare your physiology for deep, restorative sleep."
        ),
        ReflectionItem(
            id = "e_complete_02",
            timeOfDay = "EVENING",
            principle = "Reflection",
            category = "Completion",
            moods = listOf("Great", "Going well"),
            text = "You honored your commitments today. Let that quiet certainty anchor your rest."
        ),
        ReflectionItem(
            id = "e_forgive_02",
            timeOfDay = "EVENING",
            principle = "Acceptance",
            category = "Forgiveness",
            moods = listOf("Behind", "Stressed"),
            text = "Self-criticism burns calories without building character. Note the lesson calmly, forgive yourself, and rest."
        ),
        ReflectionItem(
            id = "e_responsibility_01",
            timeOfDay = "EVENING",
            principle = "Responsibility",
            category = "Reflection",
            moods = listOf("Good", "Okay"),
            text = "Ask yourself before sleeping: Did I grow in self-command today? Even an inch of progress is victory."
        ),
        ReflectionItem(
            id = "e_courage_01",
            timeOfDay = "EVENING",
            principle = "Courage",
            category = "Tomorrow",
            moods = listOf("Stressed", "Tired"),
            text = "Whatever tomorrow demands of you, you will have fresh eyes and restored strength to meet it."
        ),
        ReflectionItem(
            id = "e_gratitude_02",
            timeOfDay = "EVENING",
            principle = "Gratitude",
            category = "Gratitude",
            moods = listOf("Great", "Good"),
            text = "Count three specific things that went right today. Ending the day in appreciation primes the nervous system for peace."
        ),
        ReflectionItem(
            id = "e_control_02",
            timeOfDay = "EVENING",
            principle = "Dichotomy of Control",
            category = "Control",
            moods = listOf("Stressed", "Behind"),
            text = "Surrender what lies beyond your hands. Put down the burden of the unfinished world until morning."
        ),
        ReflectionItem(
            id = "e_virtue_02",
            timeOfDay = "EVENING",
            principle = "Virtue",
            category = "Self-Respect",
            moods = listOf("Good", "Okay"),
            text = "No man is free who is not master of himself. Every day of discipline makes you freer."
        ),
        ReflectionItem(
            id = "e_tomorrow_02",
            timeOfDay = "EVENING",
            principle = "Preparation",
            category = "Tomorrow",
            moods = listOf("Good", "Okay"),
            text = "Fill your water bottle for tomorrow morning. Set your alarm. Make the first good decision effortless."
        ),
        ReflectionItem(
            id = "e_rest_03",
            timeOfDay = "EVENING",
            principle = "Rest",
            category = "Rest",
            moods = listOf("Tired", "Stressed"),
            text = "Allow your mind to power down. There is nothing more to do today. You have done enough."
        ),
        ReflectionItem(
            id = "e_patience_02",
            timeOfDay = "EVENING",
            principle = "Patience",
            category = "Patience",
            moods = listOf("Behind", "Okay"),
            text = "Do not be discouraged by slow progress. The grandest oak was once an acorn that held its ground."
        ),
        ReflectionItem(
            id = "e_complete_03",
            timeOfDay = "EVENING",
            principle = "Reflection",
            category = "Completion",
            moods = listOf("Great", "Going well"),
            text = "Another day logged in the pursuit of your highest self. Well done. Sleep deeply."
        ),
        ReflectionItem(
            id = "e_recovery_02",
            timeOfDay = "EVENING",
            principle = "Recovery",
            category = "Recovery",
            moods = listOf("Behind", "Tired"),
            text = "The beauty of a discipline practice is that morning brings a brand new beginning. See you at sunrise."
        ),
        ReflectionItem(
            id = "e_env_03",
            timeOfDay = "EVENING",
            principle = "Discipline",
            category = "Environment",
            moods = listOf("Okay", "Good"),
            text = "External order generates internal tranquility. Keep your sanctuary simple, dark, and cool tonight."
        ),
        ReflectionItem(
            id = "e_acceptance_02",
            timeOfDay = "EVENING",
            principle = "Acceptance",
            category = "Acceptance",
            moods = listOf("Stressed", "Behind"),
            text = "Say to yourself: 'I accept everything that happened today, both the smooth and the rough.' Breathe out."
        ),
        ReflectionItem(
            id = "e_memento_02",
            timeOfDay = "EVENING",
            principle = "Memento Mori",
            category = "Reflection",
            moods = listOf("Good", "Okay"),
            text = "When you close your eyes tonight, be glad you spent this day seeking wisdom and health."
        ),
        ReflectionItem(
            id = "e_gratitude_03",
            timeOfDay = "EVENING",
            principle = "Gratitude",
            category = "Gratitude",
            moods = listOf("Great", "Good"),
            text = "Sleep is the harvest of a day lived with intention. Rest peacefully."
        )
    )

    fun getReflection(
        timeOfDay: String,
        mood: String? = null,
        recentIds: List<String> = emptyList()
    ): ReflectionItem {
        val pool = reflections.filter { it.timeOfDay.equals(timeOfDay, ignoreCase = true) }
        val unshown = pool.filterNot { it.id in recentIds }
        val candidateList = if (unshown.isNotEmpty()) unshown else pool

        // If mood is provided, prioritize matching mood
        if (!mood.isNullOrBlank()) {
            val moodMatches = candidateList.filter { item ->
                item.moods.any { it.equals(mood, ignoreCase = true) }
            }
            if (moodMatches.isNotEmpty()) {
                return moodMatches.random()
            }
        }

        return candidateList.randomOrNull() ?: pool.first()
    }
}
