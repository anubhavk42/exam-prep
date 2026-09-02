package com.anubhav.diprep.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class Subject(
    val id: String,
    val name: String,
    val description: String = "",
    val status: SubjectStatus,
    val completionPercent: Int,
    val testCount: Int = 0,
    val hasTopics: Boolean = false,
    val totalQuestionsAvailable: Int = 15,
    val highYieldTopics: List<String> = emptyList()
)

enum class SubjectStatus(val label: String) {
    MASTERED("Mastered"),
    IN_PROGRESS("In Progress"),
    NEEDS_WORK("Needs Work")
}

@Immutable
data class HabitLogItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val isCompleted: Boolean = false
)

@Immutable
data class QuizQuestion(
    val id: Int,
    val subjectId: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

object ExamDataConstants {
    val ALL_SUBJECTS = listOf(
        Subject(
            id = "pharmacology",
            name = "Pharmacology",
            description = "Mechanisms of drug action, ANS, CNS, Cardiovascular, Antimicrobial agents & Pharmacokinetics",
            status = SubjectStatus.MASTERED,
            completionPercent = 85,
            highYieldTopics = listOf("Adrenergic & Cholinergic Drugs", "Cardiovascular Agents", "Chemotherapy & Antibiotics", "Drug Interactions")
        ),
        Subject(
            id = "pharm_analysis",
            name = "Pharmaceutical Analysis",
            description = "Spectroscopy (UV, IR, NMR, Mass), Chromatography (HPLC, GC, TLC), Titrations & Quality Control",
            status = SubjectStatus.IN_PROGRESS,
            completionPercent = 62,
            highYieldTopics = listOf("HPLC & GC Retention Mechanisms", "UV-Vis Beer-Lambert Law", "Karl Fischer Titration", "IR Functional Groups")
        ),
        Subject(
            id = "jurisprudence",
            name = "Jurisprudence & Drug Laws",
            description = "Drugs & Cosmetics Act 1940 & Rules 1945, Schedules (M, X, C, H, Y), Pharmacy Act & NDPS Act",
            status = SubjectStatus.NEEDS_WORK,
            completionPercent = 30,
            highYieldTopics = listOf("Schedule M (GMP Requirements)", "Schedule X (Psychotropics)", "Drug Inspector Powers (Sec 21-23)", "Form 20/21 Licensing")
        ),
        Subject(
            id = "pharmacognosy",
            name = "Pharmacognosy",
            description = "Plant classification, Alkaloids, Glycosides, Volatile oils, Adulteration & Phytochemical screening",
            status = SubjectStatus.IN_PROGRESS,
            completionPercent = 75,
            highYieldTopics = listOf("Alkaloid Tests (Mayer, Dragendorff)", "Cardiac Glycosides (Keller-Kiliani)", "Sennosides & Anthraquinones", "Microscopical Evaluation")
        ),
        Subject(
            id = "microbiology",
            name = "Microbiology",
            description = "Sterilization methods (Autoclave, Radiation, Filtration), Disinfectant evaluation & Aseptic technique",
            status = SubjectStatus.MASTERED,
            completionPercent = 92,
            highYieldTopics = listOf("Sterilization Assurance Levels (SAL)", "Bacterial Endotoxin Test (LAL)", "Microbial Limit Tests", "Antibiotic Assays")
        ),
        Subject(
            id = "biochemistry",
            name = "Biochemistry",
            description = "Enzymes, Carbohydrate & Lipid metabolism, Protein synthesis, Vitamins & Clinical pathology",
            status = SubjectStatus.NEEDS_WORK,
            completionPercent = 42,
            highYieldTopics = listOf("Enzyme Kinetics (Km & Vmax)", "Glycolysis & Krebs Cycle", "Lipoprotein Metabolism", "Coenzymes & Vitamins")
        ),
        Subject(
            id = "pharmaceutics",
            name = "Pharmaceutics",
            description = "Dosage forms (Tablets, Capsules, Parenterals, Novel Drug Delivery), Stability testing & Bioavailability",
            status = SubjectStatus.IN_PROGRESS,
            completionPercent = 70,
            highYieldTopics = listOf("Tablet Defects & Dissolution Testing", "Parenteral Formulation & Pyrogens", "ICH Stability Guidelines", "BCS Classification")
        )
    )

    val REAL_EXAM_QUESTIONS = listOf(
        QuizQuestion(
            id = 1,
            subjectId = "jurisprudence",
            question = "Under the Drugs and Cosmetics Act 1940, which Schedule prescribes Good Manufacturing Practices (GMP) and requirements of premises, plant and equipment?",
            options = listOf("Schedule H", "Schedule M", "Schedule X", "Schedule C"),
            correctIndex = 1,
            explanation = "Schedule M of the Drugs and Cosmetics Rules 1945 prescribes Good Manufacturing Practices (GMP) and requirements of premises, plant, and equipment for pharmaceutical products."
        ),
        QuizQuestion(
            id = 2,
            subjectId = "jurisprudence",
            question = "Which section of the Drugs and Cosmetics Act 1940 outlines the official Powers of Drug Inspectors to inspect premises and seize records?",
            options = listOf("Section 18", "Section 21", "Section 22", "Section 33"),
            correctIndex = 2,
            explanation = "Section 22 defines the Powers of Inspectors, including inspecting premises, taking samples, searching, and seizing prohibited drugs."
        ),
        QuizQuestion(
            id = 3,
            subjectId = "pharmacology",
            question = "Which enzyme is specifically inhibited by the antibacterial agent Ciprofloxacin?",
            options = listOf("DNA Polymerase III", "DNA Gyrase (Topoisomerase II)", "RNA Polymerase", "Transpeptidase"),
            correctIndex = 1,
            explanation = "Fluoroquinolones like Ciprofloxacin inhibit bacterial DNA Gyrase (topoisomerase II) and Topoisomerase IV, preventing DNA replication."
        ),
        QuizQuestion(
            id = 4,
            subjectId = "pharmacology",
            question = "Which of the following is the drug of choice for treating acute anaphylactic shock?",
            options = listOf("Atropine", "Epinephrine (Adrenaline)", "Diphenhydramine", "Hydrocortisone"),
            correctIndex = 1,
            explanation = "Epinephrine is the primary drug of choice for anaphylaxis due to its alpha-1 vasoconstriction, beta-1 cardiac stimulation, and beta-2 bronchodilation."
        ),
        QuizQuestion(
            id = 5,
            subjectId = "pharm_analysis",
            question = "In High-Performance Liquid Chromatography (HPLC), Reverse-Phase chromatography utilizes:",
            options = listOf(
                "Non-polar stationary phase and polar mobile phase",
                "Polar stationary phase and non-polar mobile phase",
                "Cation exchange stationary phase with buffer",
                "Silica gel stationary phase with hexane"
            ),
            correctIndex = 0,
            explanation = "Reverse-Phase HPLC (RP-HPLC) uses a non-polar stationary phase (like C18/ODS) and a polar mobile phase (like water/methanol/acetonitrile)."
        ),
        QuizQuestion(
            id = 6,
            subjectId = "microbiology",
            question = "The biological indicator organism used to validate moist heat sterilization (Autoclaving) is:",
            options = listOf(
                "Bacillus subtilis",
                "Geobacillus stearothermophilus",
                "Clostridium tetani",
                "Staphylococcus aureus"
            ),
            correctIndex = 1,
            explanation = "Spores of Geobacillus stearothermophilus are standard biological indicators for moist heat sterilization (121°C autoclaving)."
        ),
        QuizQuestion(
            id = 7,
            subjectId = "pharmacognosy",
            question = "The Keller-Kiliani test is specifically performed to detect which active constituent?",
            options = listOf("Tropane alkaloids", "Digitoxose (Cardiac glycosides)", "Anthraquinone glycosides", "Flavonoids"),
            correctIndex = 1,
            explanation = "The Keller-Kiliani test produces a reddish-brown ring with a bluish-green upper layer indicating 2-deoxysugars (digitoxose) in cardiac glycosides."
        ),
        QuizQuestion(
            id = 8,
            subjectId = "biochemistry",
            question = "Which enzyme catalyzes the committed and rate-limiting step in Glycolysis?",
            options = listOf("Hexokinase", "Phosphofructokinase-1 (PFK-1)", "Pyruvate kinase", "Aldolase"),
            correctIndex = 1,
            explanation = "Phosphofructokinase-1 (PFK-1) converts Fructose-6-Phosphate to Fructose-1,6-Bisphosphate and is the key rate-limiting enzyme in glycolysis."
        ),
        QuizQuestion(
            id = 9,
            subjectId = "pharmaceutics",
            question = "According to the Biopharmaceutics Classification System (BCS), Class II drugs exhibit:",
            options = listOf(
                "High Solubility, High Permeability",
                "Low Solubility, High Permeability",
                "High Solubility, Low Permeability",
                "Low Solubility, Low Permeability"
            ),
            correctIndex = 1,
            explanation = "BCS Class II drugs have Low Solubility and High Permeability, meaning dissolution is usually the rate-limiting step for oral absorption."
        ),
        QuizQuestion(
            id = 10,
            subjectId = "jurisprudence",
            question = "Which Schedule under the D&C Rules lists drugs that must be marketed and sold ONLY under the prescription of a Registered Medical Practitioner, with warning in red?",
            options = listOf("Schedule G", "Schedule H / H1", "Schedule P", "Schedule T"),
            correctIndex = 1,
            explanation = "Schedule H and H1 contain prescription-only drugs requiring mandatory Rx labels and records."
        )
    )
}
