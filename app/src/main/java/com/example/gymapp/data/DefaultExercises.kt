package com.example.gymapp.data

import java.util.UUID

// Modello dati per il singolo esercizio
data class ExerciseItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val muscleGroup: String,
    val equipment: String,
    val isCustom: Boolean = false,
)

// Lista di esercizi hardcoded di default
val DEFAULT_EXERCISES = listOf(
    // Petto
    ExerciseItem(id = "def_1", name = "Chest press", muscleGroup = "Petto", equipment = "Macchinari"),
    ExerciseItem(id = "def_2", name = "Pectoral Machine", muscleGroup = "Petto", equipment = "Macchinari"),
    ExerciseItem(id = "def_3", name = "Upper cable chest fly", muscleGroup = "Petto", equipment = "Cavi"),
    ExerciseItem(id = "def_4", name = "Lower cable chest fly", muscleGroup = "Petto", equipment = "Cavi"),
    ExerciseItem(id = "def_5", name = "Barbell bench press", muscleGroup = "Petto", equipment = "Bilanciere"),
    ExerciseItem(id = "def_6", name = "Dumbbell bench press", muscleGroup = "Petto", equipment = "Manubri"),
    ExerciseItem(id = "def_7", name = "Incline barbell bench press", muscleGroup = "Petto", equipment = "Bilanciere"),
    ExerciseItem(id = "def_8", name = "Incline dumbbell bench press", muscleGroup = "Petto", equipment = "Manubri"),
    ExerciseItem(id = "def_9", name = "Pushups", muscleGroup = "Petto", equipment = "Corpo libero"),

    // Schiena
    ExerciseItem(id = "def_10", name = "Lat machine", muscleGroup = "Schiena", equipment = "Macchinari"),
    ExerciseItem(id = "def_11", name = "Horizontal row machine", muscleGroup = "Schiena", equipment = "Macchinari"),
    ExerciseItem(id = "def_12", name = "Vertical row machine", muscleGroup = "Schiena", equipment = "Macchinari"),
    ExerciseItem(id = "def_13", name = "Pulley row machine", muscleGroup = "Schiena", equipment = "Cavi"),
    ExerciseItem(id = "def_14", name = "Dumbbell row", muscleGroup = "Schiena", equipment = "Manubri"),
    ExerciseItem(id = "def_15", name = "Cable face pull", muscleGroup = "Schiena", equipment = "Cavi"),
    ExerciseItem(id = "def_16", name = "Cable pullover", muscleGroup = "Schiena", equipment = "Cavi"),
    ExerciseItem(id = "def_17", name = "Pullups", muscleGroup = "Schiena", equipment = "Corpo libero"),

    // Gambe
    ExerciseItem(id = "def_18", name = "Squat", muscleGroup = "Gambe", equipment = "Bilanciere"),
    ExerciseItem(id = "def_19", name = "Leg press", muscleGroup = "Gambe", equipment = "Macchinari"),
    ExerciseItem(id = "def_20", name = "Leg extension", muscleGroup = "Gambe", equipment = "Macchinari"),
    ExerciseItem(id = "def_21", name = "Leg curl", muscleGroup = "Gambe", equipment = "Macchinari"),
    ExerciseItem(id = "def_22", name = "Seated leg curl", muscleGroup = "Gambe", equipment = "Macchinari"),
    ExerciseItem(id = "def_23", name = "Lying leg curl", muscleGroup = "Gambe", equipment = "Macchinari"),
    ExerciseItem(id = "def_24", name = "Bulgarian split squat", muscleGroup = "Gambe", equipment = "Manubri"),
    ExerciseItem(id = "def_25", name = "Walking lunges", muscleGroup = "Gambe", equipment = "Manubri"),
    ExerciseItem(id = "def_26", name = "Hack squat", muscleGroup = "Gambe", equipment = "Macchinari"),
    ExerciseItem(id = "def_27", name = "Romanian deadlift", muscleGroup = "Gambe", equipment = "Bilanciere"),
    ExerciseItem(id = "def_28", name = "Deadlift", muscleGroup = "Gambe", equipment = "Bilanciere"),
    ExerciseItem(id = "def_29", name = "Standing calf raise", muscleGroup = "Gambe", equipment = "Macchinari"),
    ExerciseItem(id = "def_30", name = "Seated calf raise", muscleGroup = "Gambe", equipment = "Macchinari"),
    ExerciseItem(id = "def_31", name = "Hip thrust", muscleGroup = "Gambe", equipment = "Bilanciere"),
    ExerciseItem(id = "def_32", name = "Abductor machine", muscleGroup = "Gambe", equipment = "Macchinari"),
    ExerciseItem(id = "def_33", name = "Adductor machine", muscleGroup = "Gambe", equipment = "Macchinari"),

    // Spalle
    ExerciseItem(id = "def_34", name = "Overhead press", muscleGroup = "Spalle", equipment = "Bilanciere"),
    ExerciseItem(id = "def_35", name = "Military press", muscleGroup = "Spalle", equipment = "Bilanciere"),
    ExerciseItem(id = "def_36", name = "Dumbbell shoulder press", muscleGroup = "Spalle", equipment = "Manubri"),
    ExerciseItem(id = "def_37", name = "Machine shoulder press", muscleGroup = "Spalle", equipment = "Macchinari"),
    ExerciseItem(id = "def_38", name = "Arnold press", muscleGroup = "Spalle", equipment = "Manubri"),
    ExerciseItem(id = "def_39", name = "Lateral raise", muscleGroup = "Spalle", equipment = "Manubri"),
    ExerciseItem(id = "def_40", name = "Cable lateral raise", muscleGroup = "Spalle", equipment = "Cavi"),
    ExerciseItem(id = "def_41", name = "Front raise", muscleGroup = "Spalle", equipment = "Manubri"),
    ExerciseItem(id = "def_42", name = "Reverse pec deck", muscleGroup = "Spalle", equipment = "Macchinari"),
    ExerciseItem(id = "def_43", name = "Upright row", muscleGroup = "Spalle", equipment = "Bilanciere"),
    ExerciseItem(id = "def_44", name = "Dumbbell shrugs", muscleGroup = "Spalle", equipment = "Manubri"),

    // Tricipiti
    ExerciseItem(id = "def_45", name = "Tricep pushdown", muscleGroup = "Tricipiti", equipment = "Cavi"),
    ExerciseItem(id = "def_46", name = "Cable tricep pushdown", muscleGroup = "Tricipiti", equipment = "Cavi"),
    ExerciseItem(id = "def_47", name = "Skullcrusher", muscleGroup = "Tricipiti", equipment = "Bilanciere"),
    ExerciseItem(id = "def_48", name = "Close grip bench press", muscleGroup = "Tricipiti", equipment = "Bilanciere"),
    ExerciseItem(id = "def_49", name = "Cable overhead tricep extension", muscleGroup = "Tricipiti", equipment = "Cavi"),
    ExerciseItem(id = "def_50", name = "Dumbbell overhead tricep extension", muscleGroup = "Tricipiti", equipment = "Manubri"),
    ExerciseItem(id = "def_51", name = "Tricep kickback", muscleGroup = "Tricipiti", equipment = "Manubri"),
    ExerciseItem(id = "def_52", name = "Tricep dips", muscleGroup = "Tricipiti", equipment = "Corpo libero"),

    // Bicipiti
    ExerciseItem(id = "def_53", name = "Barbell curl", muscleGroup = "Bicipiti", equipment = "Bilanciere"),
    ExerciseItem(id = "def_54", name = "Dumbbell curl", muscleGroup = "Bicipiti", equipment = "Manubri"),
    ExerciseItem(id = "def_55", name = "Hammer curl", muscleGroup = "Bicipiti", equipment = "Manubri"),
    ExerciseItem(id = "def_56", name = "Cable curl", muscleGroup = "Bicipiti", equipment = "Cavi"),
    ExerciseItem(id = "def_57", name = "Preacher curl", muscleGroup = "Bicipiti", equipment = "Bilanciere"),
    ExerciseItem(id = "def_58", name = "Machine preacher curl", muscleGroup = "Bicipiti", equipment = "Macchinari"),
    ExerciseItem(id = "def_59", name = "Concentration curl", muscleGroup = "Bicipiti", equipment = "Manubri"),
    ExerciseItem(id = "def_60", name = "Reverse curl", muscleGroup = "Bicipiti", equipment = "Bilanciere"),
    ExerciseItem(id = "def_61", name = "EZ bar curl", muscleGroup = "Bicipiti", equipment = "Bilanciere")
)
