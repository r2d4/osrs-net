import cv2
import os
import json

size = 640

for file in os.listdir("testdata"):
    if file.endswith(".png"):
        image = cv2.imread(os.path.join("testdata", file))
        window_name = 'Image'
        base = os.path.splitext(file)[0]
        color = (0, 0, 255)
        thickness = 1

        # with open(os.path.join("testdata", "test.json"), 'r') as f:
        #     j = json.load(f)
        #     for l in j['labels']:
        #         b = l['boundingBox']

        #         x = b['x'] - size
        #         y = b['y'] - 156

        #         width = b['width']
        #         height = b['height']

        #         if x < 0:
        #             width = width + x
        #             x = 0

        #         if y < 0:
        #             height = height + y
        #             y = 0

        #         if (x + width) > size:
        #             width = size - x

        #         if (y + height) > size:
        #             height = size - y

        #         start_point = (int(x), int(y))
        #         end_point = (int(x + width), int(y + height))
        #         image = cv2.rectangle(image, start_point,
        #                               end_point, color, thickness)

        #         center = (int(x + width/2), int(y + height/2))
        #         radius = 2
        #         image = cv2.circle(image, center, radius, color, thickness)

        with open(os.path.join("images", base + ".txt"), 'r') as f:
            while True:
                line = f.readline()

                if not line:
                    break
                line = line.split(" ")
                classname = line[0]
                print(line[1])
                x_center = float(line[1]) * size
                y_center = float(line[2]) * size
                width = float(line[3]) * size
                height = float(line[4]) * size

                start_point = (int(x_center - width), int(y_center - height))
                end_point = (int(x_center + width), int(y_center + height))
                image = cv2.rectangle(image, start_point,
                                      end_point, color, thickness)

                center = (int(x_center), int(y_center))
                image = cv2.circle(image, center, 1, color, thickness)

        cv2.imshow(window_name, image)
        cv2.waitKey(0)
