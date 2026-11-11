import argparse
from pathlib import Path

import numpy as np
import tensorflow as tf

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("dataset", type=Path, help="Carpeta que contiene train, val y test")
    parser.add_argument("--epochs", type=int, default=15)
    args = parser.parse_args()
    tf.keras.utils.set_random_seed(42)
    labels = sorted(p.name for p in (args.dataset / "train").iterdir() if p.is_dir())
    if "__otras__" not in labels or len(labels) < 3:
        parser.error("Incluye dos especies como mínimo y la clase __otras__.")
    sets = []
    for split in ("train", "val", "test"):
        folder = args.dataset / split
        if sorted(p.name for p in folder.iterdir() if p.is_dir()) != labels:
            parser.error(f"{split} debe contener las mismas clases que train")
        data = tf.keras.utils.image_dataset_from_directory(
            folder, class_names=labels, image_size=(224, 224),
            batch_size=32, shuffle=split == "train", seed=42,
        )
        sets.append(data.prefetch(tf.data.AUTOTUNE))
    train, val, test = sets
    base = tf.keras.applications.MobileNetV2(
        input_shape=(224, 224, 3), include_top=False, weights="imagenet"
    )
    base.trainable = False
    inputs = tf.keras.Input((224, 224, 3))
    x = tf.keras.layers.RandomFlip("horizontal")(inputs)
    x = tf.keras.layers.RandomRotation(0.06)(x)
    x = tf.keras.layers.Rescaling(1 / 127.5, offset=-1)(x)
    x = base(x, training=False)
    x = tf.keras.layers.GlobalAveragePooling2D()(x)
    x = tf.keras.layers.Dropout(0.2)(x)
    outputs = tf.keras.layers.Dense(len(labels), activation="softmax")(x)
    model = tf.keras.Model(inputs, outputs)
    model.compile(optimizer="adam", loss="sparse_categorical_crossentropy", metrics=["accuracy"])
    model.fit(train, validation_data=val, epochs=args.epochs, callbacks=[
        tf.keras.callbacks.EarlyStopping(patience=4, restore_best_weights=True)
    ])
    print("Evaluación de Keras", model.evaluate(test, return_dict=True))
    export = Path(__file__).resolve().parent / "saved_model"
    model.export(str(export))
    converter = tf.lite.TFLiteConverter.from_saved_model(str(export))
    converted = converter.convert()
    interpreter = tf.lite.Interpreter(model_content=converted)
    interpreter.allocate_tensors()
    source = interpreter.get_input_details()[0]["index"]
    target = interpreter.get_output_details()[0]["index"]
    matrix = np.zeros((len(labels), len(labels)), dtype=int)
    uncertain = 0
    for images, targets in test:
        for image, actual in zip(images.numpy(), targets.numpy()):
            interpreter.set_tensor(source, image[None].astype(np.float32))
            interpreter.invoke()
            scores = interpreter.get_tensor(target)[0]
            guess = int(np.argmax(scores))
            matrix[actual, guess] += 1
            uncertain += int(guess == labels.index("__otras__") or scores[guess] < 0.55)
    print("Clases:", labels)
    print("Matriz TFLite (filas reales; columnas predichas):\n", matrix)
    print("Exactitud TFLite:", matrix.trace() / matrix.sum())
    print("Imágenes rechazadas o inciertas:", uncertain)
    assets = export.parent.parent / "app" / "src" / "main" / "assets"
    (assets / "plantas.tflite").write_bytes(converted)
    (assets / "labels.txt").write_text("\n".join(labels) + "\n", encoding="utf-8")

if __name__ == "__main__":
    main()
