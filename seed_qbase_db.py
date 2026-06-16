import sqlite3
import os

DB_PATH = 'app/src/main/assets/database/qbase.db'

# Define questions to add
# Format of question:
# (question_id, master_category, category, tags, question_type, stem, options, correct_answer_string, general_explanation, reference)
# options: list of (option_letter, option_text)

questions_to_add = []

# ==========================================
# 1. MATHEMATICS (18 SBA, 18 MTF)
# ==========================================
# Current: math_001, math_002 (SBA), math_003, math_004 (MTF)
# Adding math_sba_005 to math_sba_022
# Adding math_mtf_005 to math_mtf_022

math_categories = ['Arithmetic', 'Algebra', 'Number Properties', 'Geometry']

# Math SBA questions (18 questions)
math_sba_data = [
    ("What is the sum of the first 10 positive integers?", "Arithmetic", "math, sum", "55", "The sum formula is n(n+1)/2. For n=10, 10*11/2 = 55.", "A", ["55", "50", "45", "60"]),
    ("Solve for x in the equation 3x - 7 = 14.", "Algebra", "math, algebra", "7", "3x = 14 + 7 => 3x = 21 => x = 7.", "B", ["5", "7", "9", "6"]),
    ("What is the area of a circle with a radius of 7 units? (Use pi = 22/7)", "Geometry", "math, geometry", "154", "Area = pi * r^2 = (22/7) * 7 * 7 = 154.", "A", ["154", "44", "98", "148"]),
    ("Which of the following is a prime number?", "Number Properties", "math, primes", "23", "23 is only divisible by 1 and itself.", "C", ["15", "21", "23", "27"]),
    ("Calculate the product of 12 and 15.", "Arithmetic", "math, product", "180", "12 * 15 = 180.", "D", ["150", "165", "175", "180"]),
    ("Simplify: 2^3 * 2^2.", "Algebra", "math, exponent", "32", "2^3 * 2^2 = 2^(3+2) = 2^5 = 32.", "B", ["16", "32", "64", "12"]),
    ("Find the perimeter of a rectangle with length 10cm and width 5cm.", "Geometry", "math, geometry", "30cm", "Perimeter = 2 * (length + width) = 2 * (10 + 5) = 30cm.", "A", ["30cm", "15cm", "50cm", "45cm"]),
    ("What is the least common multiple (LCM) of 6 and 8?", "Number Properties", "math, lcm", "24", "Multiples of 6: 6, 12, 18, 24. Multiples of 8: 8, 16, 24.", "C", ["12", "18", "24", "48"]),
    ("Evaluate: 100 - (45 / 5) * 3.", "Arithmetic", "math, arithmetic", "73", "100 - 9 * 3 = 100 - 27 = 73.", "D", ["91", "85", "64", "73"]),
    ("What is the value of x if x^2 - 9 = 0 and x > 0?", "Algebra", "math, algebra", "3", "x^2 = 9 => x = 3 (since x > 0).", "A", ["3", "-3", "9", "1"]),
    ("What is the sum of the interior angles of a triangle?", "Geometry", "math, angles", "180 degrees", "The sum of interior angles of a triangle is always 180 degrees.", "B", ["90 degrees", "180 degrees", "360 degrees", "270 degrees"]),
    ("If a number is divisible by both 2 and 3, it must be divisible by:", "Number Properties", "math, divisibility", "6", "If a number is divisible by co-prime factors, it is divisible by their product.", "C", ["5", "9", "6", "8"]),
    ("Convert 3/5 to a decimal.", "Arithmetic", "math, fractions", "0.6", "3/5 = 6/10 = 0.6.", "A", ["0.6", "0.3", "0.5", "0.15"]),
    ("Solve for y: 2y + 4 = 10.", "Algebra", "math, algebra", "3", "2y = 10 - 4 => 2y = 6 => y = 3.", "D", ["2", "4", "5", "3"]),
    ("Find the volume of a cube with side length 3 units.", "Geometry", "math, volume", "27", "Volume = side^3 = 3^3 = 27.", "C", ["9", "18", "27", "81"]),
    ("What is the greatest common divisor (GCD) of 12 and 18?", "Number Properties", "math, gcd", "6", "Factors of 12: 1,2,3,4,6,12. Factors of 18: 1,2,3,6,9,18.", "B", ["3", "6", "2", "12"]),
    ("Subtract 4.56 from 10.", "Arithmetic", "math, decimals", "5.44", "10 - 4.56 = 5.44.", "A", ["5.44", "6.44", "5.54", "6.54"]),
    ("What is the value of 5! (factorial of 5)?", "Arithmetic", "math, factorial", "120", "5! = 5 * 4 * 3 * 2 * 1 = 120.", "C", ["24", "60", "120", "720"])
]

for idx, data in enumerate(math_sba_data):
    stem, cat, tag, ans, exp, correct_letter, opts = data
    qid = f"math_sba_{idx+5:03d}"
    options_list = []
    for letter_idx, opt_text in enumerate(opts):
        letter = chr(65 + letter_idx)
        options_list.append((letter, opt_text))
    questions_to_add.append((qid, "Mathematics", cat, tag, "SBA", stem, options_list, correct_letter, exp, "General Mathematics"))

# Math MTF questions (18 questions)
math_mtf_data = [
    ("Select the statements that are true regarding prime numbers:", "Number Properties", "math, primes", "B,D", "2 is prime and even. 1 is not prime. 2 and 3 are consecutive primes. All primes except 2 are odd.", 
     ["1 is the smallest prime number.", "2 is the only even prime number.", "All prime numbers are odd.", "There are consecutive numbers that are prime."]),
    ("Identify the linear equations in the following options:", "Algebra", "math, equations", "A,C", "A and C are linear. B is quadratic. D has x in the denominator.", 
     ["y = 2x + 3", "y = x^2 - 1", "3x + 2y = 5", "y = 1/x"]),
    ("Which of the following figures are quadrilaterals?", "Geometry", "math, geometry", "A,B,D", "Square, Rectangle, and Trapezoid are quadrilaterals. Triangle is not.", 
     ["Square", "Rectangle", "Triangle", "Trapezoid"]),
    ("Choose the fractions that are equivalent to 1/2:", "Arithmetic", "math, fractions", "B,C,D", "2/4, 3/6, and 50/100 simplify to 1/2. 3/5 does not.", 
     ["3/5", "2/4", "3/6", "50/100"]),
    ("Which of the following numbers are perfect squares?", "Number Properties", "math, squares", "A,C,D", "16=4^2, 25=5^2, 81=9^2. 24 is not a perfect square.", 
     ["16", "24", "25", "81"]),
    ("Evaluate properties of a right-angled triangle:", "Geometry", "math, triangles", "A,C", "Pythagorean theorem applies. The hypotenuse is the longest side. Sum of angles is 180 (so sum of other two is 90, not 45).", 
     ["The square of the hypotenuse equals the sum of squares of the other sides.", "It must have two 45-degree angles.", "The hypotenuse is opposite to the 90-degree angle.", "The area is calculated as length * width."]),
    ("Which of the following are rational numbers?", "Number Properties", "math, numbers", "A,B,C", "0.5, -4, and 2/3 are rational. Pi is irrational.", 
     ["0.5", "-4", "2/3", "Pi"]),
    ("Identify algebraic identities that are correct:", "Algebra", "math, algebra", "A,D", "A is correct. B is incorrect (needs -2ab). C is incorrect. D is correct.", 
     ["(a + b)^2 = a^2 + 2ab + b^2", "(a - b)^2 = a^2 - b^2", "a^2 - b^2 = (a - b)^2", "a^2 - b^2 = (a - b)(a + b)"]),
    ("Which operations are commutative?", "Arithmetic", "math, operations", "A,C", "Addition and multiplication are commutative. Subtraction and division are not.", 
     ["Addition", "Subtraction", "Multiplication", "Division"]),
    ("Identify the even numbers in the list:", "Number Properties", "math, parity", "A,D", "0 and 24 are even. 15 and 99 are odd.", 
     ["0", "15", "99", "24"]),
    ("Which coordinates lie in the second quadrant (x < 0, y > 0)?", "Geometry", "math, geometry", "B,C", "(-3, 4) and (-1, 2) have negative x and positive y.", 
     ["(3, 4)", "(-3, 4)", "(-1, 2)", "(-5, -6)"]),
    ("Identify terms that are factors of 24:", "Number Properties", "math, factors", "A,B,D", "6, 8, and 12 divide 24. 9 does not.", 
     ["6", "8", "9", "12"]),
    ("Which of the following are polygons?", "Geometry", "math, geometry", "A,B,C", "Triangle, Hexagon, and Pentagon are polygons. Circle is not.", 
     ["Triangle", "Hexagon", "Circle", "Pentagon"]),
    ("Identify algebraic inequalities:", "Algebra", "math, algebra", "B,C", "Inequalities use <, >, <=, >=. Equations use =.", 
     ["x = 5", "x > 3", "2x + 1 <= 9", "y = x - 4"]),
    ("Which of the following numbers are divisible by 3?", "Number Properties", "math, divisibility", "A,C,D", "Sum of digits: 1+2=3 (yes), 1+5=6 (yes), 2+7=9 (yes). 13 sum=4 (no).", 
     ["12", "13", "15", "27"]),
    ("Select valid properties of a square:", "Geometry", "math, geometry", "A,B,D", "All angles are 90 degrees. All sides are equal. Diagonals bisect at 90 degrees (not 45).", 
     ["All four sides are equal.", "All interior angles are 90 degrees.", "Diagonals bisect at 45 degrees.", "The diagonals are equal in length."]),
    ("Identify equivalent percentages to 1/4:", "Arithmetic", "math, percentages", "B,D", "1/4 = 0.25 = 25%.", 
     ["2.5%", "25%", "0.25%", "25.0%"]),
    ("Identify variables in the expression 3x + 5y - 7:", "Algebra", "math, algebra", "A,C", "x and y are variables. 3 and 5 are coefficients. -7 is a constant.", 
     ["x", "3", "y", "-7"])
]

for idx, data in enumerate(math_mtf_data):
    stem, cat, tag, ans_str, exp, opts = data
    qid = f"math_mtf_{idx+5:03d}"
    options_list = []
    for letter_idx, opt_text in enumerate(opts):
        letter = chr(65 + letter_idx)
        options_list.append((letter, opt_text))
    questions_to_add.append((qid, "Mathematics", cat, tag, "MTF", stem, options_list, ans_str, exp, "General Mathematics"))


# ==========================================
# 2. ARTIFICIAL INTELLIGENCE (18 SBA, 18 MTF)
# ==========================================
# Current: ai_001, ai_002 (SBA), ai_003, ai_004 (MTF)

ai_categories = ['Machine Learning', 'Generative AI', 'Deep Learning', 'Computing Systems']

# AI SBA questions (18 questions)
ai_sba_data = [
    ("What does 'Overfitting' refer to in Machine Learning?", "Machine Learning", "ai, ml", "Model performs well on training data but poorly on unseen data", "Overfitting occurs when a model learns the noise in training data.", "A", ["Model performs well on training data but poorly on unseen data", "Model performs poorly on both training and test data", "Model performs perfectly on all data", "Model training is too fast"]),
    ("Which neural network architecture is best suited for image processing?", "Deep Learning", "ai, dl, images", "Convolutional Neural Network (CNN)", "CNNs are designed to capture spatial hierarchy in images.", "C", ["Recurrent Neural Network (RNN)", "Transformer", "Convolutional Neural Network (CNN)", "Multilayer Perceptron (MLP)"]),
    ("What does the 'G' in GPT stand for?", "Generative AI", "ai, genai", "Generative", "GPT stands for Generative Pre-trained Transformer.", "B", ["Global", "Generative", "Graduated", "Graphical"]),
    ("Which of the following is a hardware accelerator designed specifically for neural networks?", "Computing Systems", "ai, hardware", "TPU (Tensor Processing Unit)", "TPUs are custom ASIC accelerators developed by Google for machine learning.", "D", ["CPU", "SATA SSD", "Sound Card", "TPU (Tensor Processing Unit)"]),
    ("Which evaluation metric is used for classification problems?", "Machine Learning", "ai, ml, metrics", "F1 Score", "F1 Score is a classification metric. Mean Squared Error is for regression.", "B", ["Mean Squared Error (MSE)", "F1 Score", "R-squared", "Mean Absolute Error"]),
    ("What activation function outputs values in the range [0, 1]?", "Deep Learning", "ai, dl", "Sigmoid", "Sigmoid maps input to [0,1]. Tanh maps to [-1,1]. ReLU maps to [0, inf).", "A", ["Sigmoid", "ReLU", "Tanh", "Softmax"]),
    ("In Generative AI, what is the purpose of 'Temperature'?", "Generative AI", "ai, genai", "Controls the randomness of the model output", "Higher temperature increases randomness; lower temperature makes output more deterministic.", "C", ["Controls the speed of generating tokens", "Sets the hardware heating limits", "Controls the randomness of the model output", "Limits the maximum context window size"]),
    ("Which type of learning uses rewards and punishments to train an agent?", "Machine Learning", "ai, ml", "Reinforcement Learning", "Reinforcement learning is based on trial and error with reward feedback.", "B", ["Supervised Learning", "Reinforcement Learning", "Unsupervised Learning", "Semi-supervised Learning"]),
    ("What does API stand for in software and AI services?", "Computing Systems", "ai, systems", "Application Programming Interface", "API is Application Programming Interface.", "D", ["Artificial Program Integration", "Advanced Processing Interface", "Automated Pattern Inference", "Application Programming Interface"]),
    ("What algorithm is typically used to optimize weights in a neural network?", "Deep Learning", "ai, dl", "Gradient Descent", "Gradient descent (and variants like Adam) is used for optimizing loss functions.", "A", ["Gradient Descent", "K-Means", "Dijkstra's Algorithm", "Apriori"]),
    ("Which of the following is an unsupervised learning task?", "Machine Learning", "ai, ml", "Clustering", "Clustering (like K-means) does not use labeled target data.", "C", ["Spam Detection", "House Price Prediction", "Clustering", "Image Classification"]),
    ("What represents the maximum length of text a model can process at once?", "Generative AI", "ai, genai", "Context Window", "Context window defines the input/output limit of tokens.", "B", ["Learning Rate", "Context Window", "Batch Size", "Parameters"]),
    ("What component in a Transformer allows it to focus on different words in a sentence?", "Deep Learning", "ai, dl, transformer", "Self-Attention", "Self-attention mechanism captures dependencies between words regardless of distance.", "A", ["Self-Attention", "Pooling layer", "Convolution", "Recurrent cell"]),
    ("Which python library is standard for loading and manipulating dataframes in ML?", "Machine Learning", "ai, tools", "Pandas", "Pandas is used for data manipulation and analysis.", "D", ["Matplotlib", "TensorFlow", "PyTorch", "Pandas"]),
    ("What does CUDA enable?", "Computing Systems", "ai, gpu", "NVIDIA GPU acceleration for computing tasks", "CUDA is NVIDIA's parallel computing platform and programming model.", "B", ["CPU caching optimizations", "NVIDIA GPU acceleration for computing tasks", "Database index compression", "AI model decryption"]),
    ("What is the main goal of Dimensionality Reduction?", "Machine Learning", "ai, ml", "Reduce the number of input variables in a dataset", "Dimensionality reduction (e.g. PCA) simplifies data while retaining variance.", "A", ["Reduce the number of input variables in a dataset", "Increase the training speed by adding dummy variables", "Eliminate duplicate rows", "Normalize labels"]),
    ("Which of the following is a Generative AI model type?", "Generative AI", "ai, genai", "Diffusion Model", "Diffusion models are generative models used primarily for image generation.", "C", ["Support Vector Machine", "Decision Tree", "Diffusion Model", "Linear Regression"]),
    ("What is gradient vanishing?", "Deep Learning", "ai, dl", "Gradients become extremely small, preventing network weights from changing", "Vanishing gradient problem happens in deep networks where gradients shrink exponentially as they backpropagate.", "B", ["Gradients grow too large, causing instability", "Gradients become extremely small, preventing network weights from changing", "Loss values go to zero instantly", "Weights become infinity"])
]

for idx, data in enumerate(ai_sba_data):
    stem, cat, tag, ans, exp, correct_letter, opts = data
    qid = f"ai_sba_{idx+5:03d}"
    options_list = []
    for letter_idx, opt_text in enumerate(opts):
        letter = chr(65 + letter_idx)
        options_list.append((letter, opt_text))
    questions_to_add.append((qid, "Artificial Intelligence", cat, tag, "SBA", stem, options_list, correct_letter, exp, "AI Knowledge Base"))

# AI MTF questions (18 questions)
ai_mtf_data = [
    ("Which of the following are supervised machine learning tasks?", "Machine Learning", "ai, ml", "A,D", "Regression and Classification are supervised (need labels). Clustering and Dimensionality Reduction are unsupervised.", 
     ["Regression", "Clustering", "Dimensionality Reduction", "Classification"]),
    ("Identify deep learning frameworks from the options below:", "Deep Learning", "ai, dl", "B,C", "PyTorch and TensorFlow are DL frameworks. Pandas is for data science. NumPy is for numerical math.", 
     ["Pandas", "PyTorch", "TensorFlow", "NumPy"]),
    ("Which techniques are commonly used to prevent overfitting in neural networks?", "Deep Learning", "ai, dl", "B,C,D", "Dropout, L1/L2 regularization, and Early Stopping prevent overfitting. High learning rate does not.", 
     ["Setting learning rate to 10.0", "Dropout", "L2 Regularization", "Early Stopping"]),
    ("Identify generative AI models and architectures:", "Generative AI", "ai, genai", "A,B,D", "GANs, VAEs, and Transformers are generative model bases. SVM is a discriminative classifier.", 
     ["Generative Adversarial Networks (GANs)", "Variational Autoencoders (VAEs)", "Support Vector Machines (SVM)", "Transformers"]),
    ("Select characteristics of Loss Functions in ML:", "Machine Learning", "ai, ml", "B,D", "Loss function measures how bad the model is (not accuracy). Optimization aims to minimize the loss.", 
     ["They should always output negative values.", "They measure the error between predictions and ground truth.", "They are used only during evaluation/testing.", "Optimization algorithms aim to minimize them."]),
    ("Which hardware components are crucial for training large AI models?", "Computing Systems", "ai, hardware", "A,C", "GPUs and TPUs are crucial accelerators. Sound card and DVD drive are irrelevant.", 
     ["GPU (Graphics Processing Unit)", "Sound Card", "TPU (Tensor Processing Unit)", "DVD Drive"]),
    ("Identify Large Language Models (LLMs) developed by major tech organizations:", "Generative AI", "ai, genai", "A,C,D", "Gemini (Google), Llama (Meta), GPT-4 (OpenAI) are LLMs. ResNet is an image CNN model.", 
     ["Gemini", "ResNet", "Llama", "GPT-4"]),
    ("What are valid activation functions in neural networks?", "Deep Learning", "ai, dl", "A,B,C", "ReLU, Sigmoid, and Tanh are activation functions. Adam is an optimizer.", 
     ["ReLU", "Sigmoid", "Tanh", "Adam"]),
    ("Which of the following are hyperparameters in machine learning?", "Machine Learning", "ai, ml", "B,C,D", "Learning rate, batch size, and number of layers are set before training. Neural network weights are learned parameters.", 
     ["Weights of the neurons", "Learning Rate", "Batch Size", "Number of hidden layers"]),
    ("Identify unsupervised machine learning methods:", "Machine Learning", "ai, ml", "B,C", "K-Means and PCA are unsupervised. Logistic Regression and Random Forest are supervised.", 
     ["Logistic Regression", "K-Means", "Principal Component Analysis (PCA)", "Random Forest"]),
    ("Identify components of a Transformer architecture:", "Deep Learning", "ai, dl", "A,B,D", "Multi-Head Attention, Feed-Forward Networks, and Positional Encoding are part of Transformers. Convolutional filters are from CNNs.", 
     ["Multi-Head Attention", "Feed-Forward Neural Networks", "Convolutional Filters", "Positional Encoding"]),
    ("Which of the following are cloud platforms offering ML API services?", "Computing Systems", "ai, cloud", "A,B,C", "Google Cloud (Vertex AI), AWS (SageMaker), and Microsoft Azure offer ML APIs. SQLite is a local database.", 
     ["Google Cloud Platform (GCP)", "Amazon Web Services (AWS)", "Microsoft Azure", "SQLite"]),
    ("Identify generative AI modalities:", "Generative AI", "ai, genai", "A,B,C,D", "Text, Image, Audio, and Code can all be generated using generative AI.", 
     ["Text-to-Text", "Text-to-Image", "Text-to-Audio", "Text-to-Code"]),
    ("Select the statements that are true about reinforcement learning:", "Machine Learning", "ai, ml", "A,B", "RL involves agents learning from rewards. It uses Markov Decision Processes. It is not supervised (does not rely on labeled input-output pairs).", 
     ["It involves an agent interacting with an environment.", "It aims to maximize cumulative reward.", "It is a form of supervised learning.", "It requires a complete static dataset of expert labels."]),
    ("Identify database types commonly used for Retrieval-Augmented Generation (RAG):", "Computing Systems", "ai, systems", "B,C", "Vector databases like Pinecone and Milvus are used in RAG. Relational DBs can be used but Vector DBs are the specialized standard.", 
     ["Standard File System (txt files)", "Pinecone (Vector Database)", "Milvus (Vector Database)", "Floppy Disk Storage"]),
    ("Select the statements that are true about Neural Networks:", "Deep Learning", "ai, dl", "A,C", "Adding hidden layers allows non-linear decision boundaries. Weights are initialized randomly, not all to zero (doing so prevents symmetry breaking).", 
     ["They can learn non-linear relationships.", "Weights must always be initialized to exactly zero.", "Backpropagation calculates gradients of the loss function.", "They cannot perform classification tasks."]),
    ("Identify open-weights or open-source LLMs:", "Generative AI", "ai, genai", "B,C", "Llama and Mistral are open-weight models. GPT-4 and Claude are proprietary closed APIs.", 
     ["GPT-4", "Llama 3", "Mistral 7B", "Claude 3"]),
    ("Select key phases in the machine learning lifecycle:", "Machine Learning", "ai, ml", "A,B,C,D", "Data preparation, model training, evaluation, and deployment are all key phases.", 
     ["Data collection & cleaning", "Model training", "Model evaluation", "Model deployment"])
]

for idx, data in enumerate(ai_mtf_data):
    stem, cat, tag, ans_str, exp, opts = data
    qid = f"ai_mtf_{idx+5:03d}"
    options_list = []
    for letter_idx, opt_text in enumerate(opts):
        letter = chr(65 + letter_idx)
        options_list.append((letter, opt_text))
    questions_to_add.append((qid, "Artificial Intelligence", cat, tag, "MTF", stem, options_list, ans_str, exp, "AI Knowledge Base"))


# ==========================================
# 3. GENERAL SCIENCE (18 SBA, 18 MTF)
# ==========================================
# Current: sci_001, sci_002 (SBA), sci_003, sci_004 (MTF)

sci_categories = ['Physics', 'Chemistry', 'Astronomy', 'Cell Biology']

# Science SBA questions (18 questions)
sci_sba_data = [
    ("What is the fundamental unit of life?", "Cell Biology", "science, biology", "Cell", "The cell is the basic structural and functional unit of life.", "A", ["Cell", "Atom", "Tissue", "Organ"]),
    ("Which planet is known as the Red Planet?", "Astronomy", "science, astronomy", "Mars", "Mars is called the Red Planet due to iron oxide on its surface.", "C", ["Venus", "Jupiter", "Mars", "Saturn"]),
    ("What is the chemical symbol for water?", "Chemistry", "science, chemistry", "H2O", "Water consists of two hydrogen atoms and one oxygen atom.", "B", ["CO2", "H2O", "O2", "NaCl"]),
    ("What force keeps planets in orbit around the Sun?", "Physics", "science, physics", "Gravity", "Gravity is the attractive force between masses.", "A", ["Gravity", "Electromagnetism", "Friction", "Centrifugal force"]),
    ("Which organelle is known as the powerhouse of the cell?", "Cell Biology", "science, biology", "Mitochondria", "Mitochondria produce energy (ATP) for the cell.", "D", ["Nucleus", "Ribosome", "Lysosome", "Mitochondria"]),
    ("What is the speed of light in a vacuum?", "Physics", "science, physics", "Approximately 300,000 km/s", "Light travels at approx 299,792 km/s in a vacuum.", "B", ["3,000 km/s", "Approximately 300,000 km/s", "300,000 m/s", "1,080 km/h"]),
    ("What gas do plants absorb from the atmosphere during photosynthesis?", "Cell Biology", "science, biology", "Carbon Dioxide", "Plants absorb CO2 and release Oxygen.", "C", ["Oxygen", "Nitrogen", "Carbon Dioxide", "Helium"]),
    ("Which element has the atomic number 1?", "Chemistry", "science, chemistry", "Hydrogen", "Hydrogen is the first element on the periodic table.", "A", ["Hydrogen", "Helium", "Lithium", "Oxygen"]),
    ("What is the main component of the Sun?", "Astronomy", "science, astronomy", "Hydrogen", "The Sun is made of roughly 74% Hydrogen and 24% Helium.", "B", ["Oxygen", "Hydrogen", "Iron", "Carbon"]),
    ("Which law of motion states that for every action there is an equal and opposite reaction?", "Physics", "science, physics", "Newton's Third Law", "Newton's third law of motion explains action-reaction pairs.", "C", ["Newton's First Law", "Newton's Second Law", "Newton's Third Law", "Law of Gravitation"]),
    ("What is the chemical formula for common table salt?", "Chemistry", "science, chemistry", "NaCl", "NaCl is Sodium Chloride.", "D", ["H2O", "HCl", "CO2", "NaCl"]),
    ("What is the closest star to Earth?", "Astronomy", "science, astronomy", "The Sun", "The Sun is the closest star, followed by Proxima Centauri.", "A", ["The Sun", "Proxima Centauri", "Sirius", "Alpha Centauri"]),
    ("What pigment gives leaves their green color?", "Cell Biology", "science, biology", "Chlorophyll", "Chlorophyll absorbs red and blue light, reflecting green.", "B", ["Carotenoid", "Chlorophyll", "Melanin", "Hemoglobin"]),
    ("Which of the following is a noble gas?", "Chemistry", "science, chemistry", "Helium", "Helium is in Group 18 of the periodic table, representing noble gases.", "C", ["Oxygen", "Nitrogen", "Helium", "Hydrogen"]),
    ("What is the acceleration due to gravity on Earth's surface (approximate)?", "Physics", "science, physics", "9.8 m/s^2", "Gravity acceleration is approximately 9.8 m/s^2.", "A", ["9.8 m/s^2", "9.8 km/s^2", "1.6 m/s^2", "32 m/s^2"]),
    ("How many chromosomes do normal human somatic cells contain?", "Cell Biology", "science, biology", "46", "Humans have 23 pairs of chromosomes, totaling 46.", "D", ["23", "48", "44", "46"]),
    ("Which planet is the largest in our solar system?", "Astronomy", "science, astronomy", "Jupiter", "Jupiter is the massive gas giant, larger than all other planets combined.", "B", ["Saturn", "Jupiter", "Neptune", "Earth"]),
    ("What is the pH of pure water at room temperature?", "Chemistry", "science, chemistry", "7", "Pure water is neutral with a pH of 7.", "C", ["1", "5", "7", "14"])
]

for idx, data in enumerate(sci_sba_data):
    stem, cat, tag, ans, exp, correct_letter, opts = data
    qid = f"sci_sba_{idx+5:03d}"
    options_list = []
    for letter_idx, opt_text in enumerate(opts):
        letter = chr(65 + letter_idx)
        options_list.append((letter, opt_text))
    questions_to_add.append((qid, "General Science", cat, tag, "SBA", stem, options_list, correct_letter, exp, "Science Reference"))

# Science MTF questions (18 questions)
sci_mtf_data = [
    ("Which of the following are eukaryotic cell organelles?", "Cell Biology", "science, biology", "A,B,D", "Nucleus, Mitochondria, and Golgi apparatus are organelles. Ribosomes are macromolecular complexes found in both eukaryotes and prokaryotes, but are sometimes grouped as organelles; however, nucleoids are prokaryotic. A, B, D are definitely eukaryotic membrane-bound organelles.", 
     ["Nucleus", "Mitochondria", "Nucleoid (Prokaryotic)", "Golgi Apparatus"]),
    ("Identify the chemical elements that are metals:", "Chemistry", "science, chemistry", "A,D", "Iron and Copper are metals. Helium is a noble gas, Carbon is a non-metal.", 
     ["Iron", "Helium", "Carbon", "Copper"]),
    ("Which of the following are planets in our Solar System?", "Astronomy", "science, astronomy", "A,B,D", "Earth, Neptune, and Jupiter are planets. Pluto is classified as a dwarf planet since 2006.", 
     ["Earth", "Neptune", "Pluto (Dwarf Planet)", "Jupiter"]),
    ("Select the scalar quantities from the options below:", "Physics", "science, physics", "A,C", "Mass and Temperature are scalar. Velocity and Acceleration are vectors.", 
     ["Mass", "Velocity", "Temperature", "Acceleration"]),
    ("Which processes are part of the water cycle?", "Earth Science", "science, water", "A,B,C", "Evaporation, Condensation, and Precipitation are part of the water cycle. Sublimation also is, but photosynthesis is a carbon cycle process.", 
     ["Evaporation", "Condensation", "Precipitation", "Photosynthesis"]),
    ("Identify chemical compounds (as opposed to pure elements):", "Chemistry", "science, chemistry", "B,D", "Water (H2O) and Carbon Dioxide (CO2) are compounds. Oxygen (O2) and Gold (Au) are elements.", 
     ["Oxygen gas", "Water", "Gold", "Carbon Dioxide"]),
    ("Which of the following are parts of a human plant cell (not animal cell)?", "Cell Biology", "science, biology", "A,B", "Cell Wall and Chloroplast are unique to plant cells. Mitochondria and Cell Membrane are in both plant and animal cells.", 
     ["Cell Wall", "Chloroplast", "Mitochondria", "Cell Membrane"]),
    ("Select statements that are true about gravity:", "Physics", "science, physics", "B,C", "Gravity is attractive only. It depends on mass and distance. It is weaker than electromagnetism.", 
     ["It can be repulsive.", "It depends on the mass of the objects.", "It depends on the distance between objects.", "It is the strongest force in nature."]),
    ("Identify inner rocky planets in our solar system:", "Astronomy", "science, astronomy", "A,B", "Mercury and Mars are rocky. Jupiter and Uranus are gas giants.", 
     ["Mercury", "Mars", "Jupiter", "Uranus"]),
    ("Which of the following are forms of electromagnetic radiation?", "Physics", "science, physics", "A,B,D", "Visible light, X-rays, and Radio waves are electromagnetic. Sound waves are mechanical.", 
     ["Visible Light", "X-rays", "Sound Waves", "Radio Waves"]),
    ("Identify indicators of a chemical change (reaction):", "Chemistry", "science, chemistry", "A,C,D", "Color change, gas production, and temperature change indicate chemical changes. Melting ice is a physical change.", 
     ["Color change", "Melting of ice", "Gas production (bubbles)", "Precipitate formation"]),
    ("Identify eukaryotic organisms:", "Cell Biology", "science, biology", "A,B,C", "Humans, Oak trees, and Yeast are eukaryotes. E. coli is a bacterium (prokaryotic).", 
     ["Humans", "Oak Tree", "Yeast", "Escherichia coli (E. coli)"]),
    ("Which of the following are parts of the atom's nucleus?", "Physics", "science, physics", "A,C", "Protons and Neutrons reside in the nucleus. Electrons orbit the nucleus.", 
     ["Protons", "Electrons", "Neutrons", "Photons"]),
    ("Select components of the human circulatory system:", "Cell Biology", "science, biology", "A,B,D", "Heart, Blood vessels, and Blood are part of the circulatory system. Lungs belong to the respiratory system.", 
     ["Heart", "Blood Vessels", "Lungs", "Blood"]),
    ("Identify acids from their pH values (pH < 7):", "Chemistry", "science, chemistry", "A,B", "Lemon juice (pH ~2) and Coffee (pH ~5) are acidic. Pure water (pH 7) is neutral, Bleach (pH 13) is basic.", 
     ["Lemon Juice (pH 2)", "Black Coffee (pH 5)", "Pure Water (pH 7)", "Bleach (pH 13)"]),
    ("Identify the gas giants in our solar system:", "Astronomy", "science, astronomy", "B,C", "Saturn and Neptune are gas/ice giants. Earth and Venus are terrestrial rocky planets.", 
     ["Earth", "Saturn", "Neptune", "Venus"]),
    ("Select the statements that are true about enzymes:", "Cell Biology", "science, biology", "A,C,D", "Enzymes are catalysts, they are proteins, and their activity depends on temperature/pH. They speed up reactions (not slow down).", 
     ["They act as biological catalysts.", "They slow down chemical reactions.", "They are mostly proteins.", "Their activity depends on temperature and pH."]),
    ("Identify states of matter under normal room conditions:", "Physics", "science, physics", "A,B,C", "Solid, Liquid, and Gas are standard. Plasma exists at extremely high temperatures.", 
     ["Solid", "Liquid", "Gas", "Plasma"])
]

for idx, data in enumerate(sci_mtf_data):
    stem, cat, tag, ans_str, exp, opts = data
    qid = f"sci_mtf_{idx+5:03d}"
    options_list = []
    for letter_idx, opt_text in enumerate(opts):
        letter = chr(65 + letter_idx)
        options_list.append((letter, opt_text))
    questions_to_add.append((qid, "General Science", cat, tag, "MTF", stem, options_list, ans_str, exp, "Science Reference"))


# ==========================================
# 4. WORLD HISTORY (18 SBA, 18 MTF)
# ==========================================
# Current: hist_001, hist_002 (SBA), hist_003, hist_004 (MTF)

hist_categories = ['Ancient History', 'Space Age', 'World Civilizations', 'World Wars']

# History SBA questions (18 questions)
hist_sba_data = [
    ("Who was the first emperor of the Roman Empire?", "Ancient History", "history, rome", "Augustus", "Augustus (Octavian) became the first Roman emperor in 27 BC.", "C", ["Julius Caesar", "Nero", "Augustus", "Marcus Aurelius"]),
    ("In which year did the United States land Apollo 11 on the Moon?", "Space Age", "history, space", "1969", "Neil Armstrong and Buzz Aldrin walked on the moon in July 1969.", "B", ["1965", "1969", "1972", "1975"]),
    ("Which ancient civilization built the Great Pyramid of Giza?", "World Civilizations", "history, egypt", "Egyptians", "The Great Pyramid was built as a tomb for Pharaoh Khufu.", "A", ["Egyptians", "Mesopotamians", "Romans", "Greeks"]),
    ("World War I began in which year?", "World Wars", "history, ww1", "1914", "WWI was triggered by the assassination of Archduke Franz Ferdinand in 1914.", "C", ["1912", "1918", "1914", "1939"]),
    ("Who was the primary author of the United States Declaration of Independence?", "World Civilizations", "history, us", "Thomas Jefferson", "Thomas Jefferson drafted the Declaration in 1776.", "D", ["George Washington", "Benjamin Franklin", "John Adams", "Thomas Jefferson"]),
    ("The code of Hammurabi is associated with which ancient civilization?", "Ancient History", "history, babylon", "Babylonian", "Hammurabi was the sixth king of the First Babylonian Dynasty.", "B", ["Egyptian", "Babylonian", "Greek", "Roman"]),
    ("Which nation launched the first artificial satellite, Sputnik 1, into space?", "Space Age", "history, space", "Soviet Union", "Sputnik 1 was launched by the Soviet Union on October 4, 1957.", "A", ["Soviet Union", "United States", "United Kingdom", "Germany"]),
    ("Who was the leader of the Nazi Party in Germany during World War II?", "World Wars", "history, ww2", "Adolf Hitler", "Adolf Hitler was chancellor of Germany from 1933 to 1945.", "C", ["Benito Mussolini", "Joseph Stalin", "Adolf Hitler", "Winston Churchill"]),
    ("What ancient trade route connected China to the Mediterranean world?", "World Civilizations", "history, trade", "The Silk Road", "The Silk Road was an ancient network of trade routes.", "B", ["The Spice Route", "The Silk Road", "The Amber Road", "The Royal Road"]),
    ("Which empire was ruled by Julius Caesar before his assassination?", "Ancient History", "history, rome", "Roman Republic", "Julius Caesar ruled as dictator of the Roman Republic, not the Roman Empire.", "A", ["Roman Republic", "Roman Empire", "Byzantine Empire", "Macedonian Empire"]),
    ("Who was the first human to travel into outer space?", "Space Age", "history, space", "Yuri Gagarin", "Yuri Gagarin completed a single orbit of Earth on April 12, 1961.", "D", ["Neil Armstrong", "Alan Shepard", "John Glenn", "Yuri Gagarin"]),
    ("The signing of the Magna Carta occurred in which year?", "World Civilizations", "history, uk", "1215", "King John signed the Magna Carta at Runnymede in 1215.", "C", ["1066", "1492", "1215", "1688"]),
    ("Which treaty officially ended World War I?", "World Wars", "history, ww1", "Treaty of Versailles", "The Treaty of Versailles was signed in 1919.", "A", ["Treaty of Versailles", "Treaty of Paris", "Treaty of Utrecht", "Treaty of Ghent"]),
    ("The philosopher Socrates belonged to which ancient city-state?", "Ancient History", "history, greece", "Athens", "Socrates lived and taught in Athens, Greece.", "B", ["Sparta", "Athens", "Thebes", "Corinth"]),
    ("What was the name of the NASA space program that successfully landed humans on the Moon?", "Space Age", "history, space", "Apollo", "The Apollo program was responsible for the moon landings.", "C", ["Gemini", "Mercury", "Apollo", "Shuttle"]),
    ("Which country was invaded by Germany on September 1, 1939, triggering World War II in Europe?", "World Wars", "history, ww2", "Poland", "Germany's invasion of Poland led Britain and France to declare war.", "D", ["France", "Soviet Union", "Czechoslovakia", "Poland"]),
    ("The city-state of Sparta was a major rival to Athens in which ancient wars?", "Ancient History", "history, greece", "Peloponnesian War", "The Peloponnesian War was fought between Athens and Sparta from 431 to 404 BC.", "A", ["Peloponnesian War", "Persian Wars", "Punic Wars", "Trojan War"]),
    ("Who was the British Prime Minister during the majority of World War II?", "World Wars", "history, ww2", "Winston Churchill", "Churchill led the UK from 1940 to 1945.", "B", ["Neville Chamberlain", "Winston Churchill", "Clement Attlee", "Franklin D. Roosevelt"])
]

for idx, data in enumerate(hist_sba_data):
    stem, cat, tag, ans, exp, correct_letter, opts = data
    qid = f"hist_sba_{idx+5:03d}"
    options_list = []
    for letter_idx, opt_text in enumerate(opts):
        letter = chr(65 + letter_idx)
        options_list.append((letter, opt_text))
    questions_to_add.append((qid, "World History", cat, tag, "SBA", stem, options_list, correct_letter, exp, "History Archives"))

# History MTF questions (18 questions)
hist_mtf_data = [
    ("Identify ancient empires or civilizations that existed before 500 AD:", "Ancient History", "history, ancient", "A,B,C", "Roman Empire, Han Dynasty, and Persian Empire existed before 500 AD. Ottoman Empire was founded in 1299.", 
     ["Roman Empire", "Han Dynasty", "Persian Empire", "Ottoman Empire"]),
    ("Which of the following achievements belong to the Space Age?", "Space Age", "history, space", "A,C,D", "Sputnik launch, Moon landing, and Hubble deploy are Space Age achievements. Wright brothers flight (1903) predated the Space Age.", 
     ["Launch of Sputnik 1", "Wright Brothers first powered flight", "Apollo 11 Moon landing", "Deployment of the Hubble Space Telescope"]),
    ("Identify Allied Powers during World War II:", "World Wars", "history, ww2", "A,C,D", "United States, Soviet Union, and Great Britain were Allies. Japan was an Axis power.", 
     ["United States", "Japan (Axis)", "Soviet Union", "Great Britain"]),
    ("Which of the following were ancient wonders of the world?", "World Civilizations", "history, ancient", "A,B,D", "Great Pyramid, Colossus of Rhodes, and Hanging Gardens are ancient wonders. Eiffel Tower is modern.", 
     ["Great Pyramid of Giza", "Colossus of Rhodes", "Eiffel Tower", "Hanging Gardens of Babylon"]),
    ("Select the statements that are true about the Roman Republic:", "Ancient History", "history, rome", "A,C", "It was governed by a Senate. Julius Caesar was assassinated in it. It did not have emperors (emperors started with the Roman Empire under Augustus). It fell before 500 AD.", 
     ["It was governed by a Senate and elected consuls.", "It had emperors from its very beginning.", "Julius Caesar's assassination contributed to its downfall.", "It lasted until the 20th century."]),
    ("Which space missions were manned?", "Space Age", "history, space", "A,C", "Apollo 11 and Vostok 1 (Yuri Gagarin) were manned. Voyager 1 and Curiosity are robotic probes.", 
     ["Apollo 11", "Voyager 1", "Vostok 1", "Curiosity Mars Rover"]),
    ("Identify Axis Powers during World War II:", "World Wars", "history, ww2", "A,B,C", "Germany, Italy, and Japan were the main Axis powers. Soviet Union was part of the Allies.", 
     ["Germany", "Italy", "Japan", "Soviet Union"]),
    ("Identify historic civilizations of Mesopotamia:", "World Civilizations", "history, ancient", "A,B,D", "Sumerians, Babylonians, and Assyrians were Mesopotamian. Incas were in South America.", 
     ["Sumerians", "Babylonians", "Incas", "Assyrians"]),
    ("Select events that occurred during the Cold War:", "Space Age", "history, coldwar", "A,B,D", "Cuban Missile Crisis, Space Race, and Berlin Wall construction occurred during the Cold War. French Revolution was in 1789.", 
     ["Cuban Missile Crisis", "The Space Race", "The French Revolution", "Construction of the Berlin Wall"]),
    ("Which of the following are treaties signed during or after World War II?", "World Wars", "history, treaties", "A,D", "Yalta Conference (1945) and Potsdam Agreement (1945) are WWII-related. Treaty of Versailles is WWI. Treaty of Westphalia is 1648.", 
     ["Yalta Conference agreements", "Treaty of Versailles", "Treaty of Westphalia", "Potsdam Agreement"]),
    ("Identify ancient Greek philosophers:", "Ancient History", "history, greece", "A,B,C", "Plato, Aristotle, and Socrates are Greek philosophers. Cicero was a Roman statesman.", 
     ["Plato", "Aristotle", "Socrates", "Cicero"]),
    ("Which of the following were major battles of World War II?", "World Wars", "history, ww2", "A,B,C", "Stalingrad, Midway, and D-Day (Normandy) are WWII battles. Waterloo (1815) is Napoleonic.", 
     ["Battle of Stalingrad", "Battle of Midway", "Battle of Waterloo", "Battle of Normandy (D-Day)"]),
    ("Identify United States space programs:", "Space Age", "history, space", "A,C,D", "Mercury, Gemini, and Apollo are US space programs. Sputnik was Soviet.", 
     ["Mercury", "Sputnik", "Gemini", "Apollo"]),
    ("Select historical events of the French Revolution:", "World Civilizations", "history, france", "A,B,C", "Storming of the Bastille, Reign of Terror, and Execution of Louis XVI are French Revolution events. Signing of the Magna Carta was British (1215).", 
     ["Storming of the Bastille", "The Reign of Terror", "Execution of King Louis XVI", "Signing of the Magna Carta"]),
    ("Identify empires of the Americas before European contact:", "World Civilizations", "history, americas", "A,B", "Aztec and Inca empires were pre-Columbian. Byzantine and Roman were European.", 
     ["Aztec Empire", "Inca Empire", "Byzantine Empire", "Roman Empire"]),
    ("Which of the following weapons were introduced/first used in World War I?", "World Wars", "history, WWI", "A,B,C", "Tanks, Chemical gas, and Military flamethrowers were first used on a large scale in WWI. Atomic bomb was WWII.", 
     ["Tanks", "Chemical weapon gas", "Military flamethrowers", "Atomic Bomb"]),
    ("Select statements that are true about the Renaissance:", "World Civilizations", "history, renaissance", "A,B,D", "It began in Italy. It was a cultural rebirth of classical learning. It preceded the industrial revolution, and did not happen in antiquity.", 
     ["It began in Italy (primarily Florence).", "It marked a rebirth of interest in Classical antiquity.", "It occurred during the Ancient Roman period.", "It witnessed significant scientific and artistic advancements."]),
    ("Identify historical events of the American Revolutionary War:", "World Civilizations", "history, us", "A,B,D", "Boston Tea Party, Battle of Yorktown, and Declaration of Independence are events of the US revolution. Battle of Waterloo is European.", 
     ["Boston Tea Party", "Battle of Yorktown", "Battle of Waterloo", "Declaration of Independence"])
]

for idx, data in enumerate(hist_mtf_data):
    stem, cat, tag, ans_str, exp, opts = data
    qid = f"hist_mtf_{idx+5:03d}"
    options_list = []
    for letter_idx, opt_text in enumerate(opts):
        letter = chr(65 + letter_idx)
        options_list.append((letter, opt_text))
    questions_to_add.append((qid, "World History", cat, tag, "MTF", stem, options_list, ans_str, exp, "History Archives"))


# ==========================================
# DATABASE INSERTION LOGIC
# ==========================================
print(f"Connecting to database: {DB_PATH}...")
conn = sqlite3.connect(DB_PATH)
cursor = conn.cursor()

# Check if tables exist
cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
tables = [t[0] for t in cursor.fetchall()]
print(f"Existing tables: {tables}")

# Begin transaction
try:
    for item in questions_to_add:
        qid, master_cat, sub_cat, tags, qtype, stem, options, correct_ans, explanation, reference = item
        
        # Check if question already exists
        cursor.execute("SELECT 1 FROM Questions WHERE question_id = ?", (qid,))
        if cursor.fetchone():
            # Delete if exists to overwrite
            cursor.execute("DELETE FROM Questions WHERE question_id = ?", (qid,))
            cursor.execute("DELETE FROM Question_Options WHERE question_id = ?", (qid,))
            cursor.execute("DELETE FROM Option_Explanations WHERE question_id = ?", (qid,))
            cursor.execute("DELETE FROM Answers WHERE question_id = ?", (qid,))
            
        # Insert into Questions
        cursor.execute(
            "INSERT INTO Questions (question_id, master_category, category, tags, question_type, stem) VALUES (?, ?, ?, ?, ?, ?)",
            (qid, master_cat, sub_cat, tags, qtype, stem)
        )
        
        # Insert into Question_Options
        for opt_letter, opt_text in options:
            cursor.execute(
                "INSERT INTO Question_Options (question_id, option_letter, option_text) VALUES (?, ?, ?)",
                (qid, opt_letter, opt_text)
            )
            # Insert empty explanation for each option
            cursor.execute(
                "INSERT INTO Option_Explanations (question_id, option_letter, specific_explanation) VALUES (?, ?, ?)",
                (qid, opt_letter, "")
            )
            
        # Insert into Answers
        cursor.execute(
            "INSERT INTO Answers (question_id, correct_answer_string, general_explanation, reference) VALUES (?, ?, ?, ?)",
            (qid, correct_ans, explanation, reference)
        )
        
    conn.commit()
    print(f"Successfully inserted/updated {len(questions_to_add)} questions.")
except Exception as e:
    conn.rollback()
    print(f"Error seeding database: {e}")
    raise e
finally:
    conn.close()
