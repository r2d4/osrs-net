import os
import json
import yaml
from ordered_set import OrderedSet
from utils.datasets import autosplit

data_dir = 'images'
names = OrderedSet()
dataset_yaml = {}
images = {}

img_width = 640
img_height = 640

size = 640

with open('image_labels.txt') as f:
    while True:
        line = f.readline()
        if not line:
            break
        metadata = json.loads(line)
        images[metadata['imageName']] = metadata['labels']
        for l in metadata["labels"]:
            names.add(l["name"])

dataset_yaml['nc'] = len(names)
dataset_yaml['names'] = list(names)
dataset_yaml['train'] = os.path.join(data_dir, 'autosplit_train.txt')
dataset_yaml['val'] = os.path.join(data_dir, 'autosplit_val.txt')


with open('dataset.yaml', 'w') as f:
    yaml.dump(dataset_yaml, f)


def oob(x):
    return x < 0 or x > 1


for img, labels in images.items():
    with open(os.path.join(data_dir, '{}.txt'.format(img)), 'w') as f:
        for label in labels:
            bb = label['boundingBox']

            x = bb['x'] - 640
            y = bb['y'] - 156

            width = bb['width']
            height = bb['height']

            if x < 0:
                width = width + x
                x = 0

            if y < 0:
                height = height + y
                y = 0

            if (x + width) > size:
                width = size - x

            if (y + height) > size:
                height = size - y

            class_name = names.index([label['name']])[0]
            x_norm_center = (x + (width/2))/size
            y_norm_center = (y + (height/2))/size

            width_norm = (width)/size
            height_norm = (height)/size

            if (oob(x_norm_center) or oob(y_norm_center) or oob(width_norm) or oob(height_norm)):
                print("error")
                quit()

            f.write('{class_name} {x_center} {y_center} {width} {height}\n'.format(
                class_name=class_name, x_center=x_norm_center, y_center=y_norm_center, width=width_norm, height=height_norm))


autosplit(path=data_dir)
