import os
import random
import torch
import cv2
import numpy as np

from models.experimental import attempt_load
from utils.general import non_max_suppression, scale_coords
from utils.datasets import letterbox
from utils.plots import plot_one_box

opts = {
    'weights': os.path.join("runs", "train", "osrs-net-2", "best.pt"),
    'conf_thres': 0.25,
    'iou_thres': 0.45,
}


model_path = os.path.join("runs", "train", "osrs-net-2", "weights", "best.pt")


class Label():

    def __init__(self, pt1, pt2, conf, label):
        self.pt1 = pt1
        self.pt2 = pt2
        self.conf = conf
        self.label = label

    def __str__(self):
        return f"{self.label} ({self.conf}) {self.pt1} {self.pt2}"

    def __repr__(self):
        return f"{self.label} ({self.conf}) {self.pt1} {self.pt2}"


debug = False


class Inference():

    def __init__(self, **kwargs):
        self.device = torch.device('cuda:0')
        self.model_path = kwargs.get("model_path")
        self.model = attempt_load(self.model_path, self.device)
        self.names = self.model.module.names if hasattr(
            self.model, 'module') else self.model.names
        self.colors = [[random.randint(0, 255)
                        for _ in range(3)] for _ in self.names]
        self.img_size = kwargs.get("img_size", 640)
        self.conf_thres = kwargs.get("conf_thres", 0.25)
        self.iou_thres = kwargs.get("iou_thres", 0.45)

    def predict(self, img):
        imgt = letterbox(img, new_shape=self.img_size)[0]
        imgt = imgt[:, :, ::-1].transpose(2, 0, 1)  # BGR to RGB, to 3x416x416
        imgt = np.ascontiguousarray(imgt)

        # img = torch.zeros(
        #     (1, 3, self.img_size, self.img_size), device=self.device)
        imgt = torch.from_numpy(imgt).to(self.device).float()
        imgt /= 255.0  # 0 - 255 to 0.0 - 1.0
        if imgt.ndimension() == 3:
            imgt = imgt.unsqueeze(0)
        pred = self.model(imgt)[0]
        pred = non_max_suppression(pred, self.conf_thres, self.iou_thres)

        labels = []
        for i, det in enumerate(pred):
            if len(det):
                gn = torch.tensor(img.shape)[[1, 0, 1, 0]]
                det[:, :4] = scale_coords(
                    imgt.shape[2:], det[:, :4], img.shape).round()
                for *xyxy, conf, classes in reversed(det):
                    class_name = '%s %.2f' % (self.names[int(classes)], conf)
                    pt1, pt2 = (xyxy[0], xyxy[1]), (xyxy[2], xyxy[3])
                    labels.append(Label(pt1, pt2, conf, class_name))
                    if debug:
                        plot_one_box(xyxy, img, label=label,
                                     color=self.colors[int(cls)], line_thickness=1)
                        print(xyxy[0], xyxy[1], xyxy[2], xyxy[3], conf, label)
                if debug:
                    cv2.imshow('prediction', img)
                    cv2.waitKey(0)
        return labels


m = Inference(model_path=model_path)
img = cv2.imread(os.path.join("testdata", "test.png"))
print(m.predict(img))
