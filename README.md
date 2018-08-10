# OEE-Designer
The OEE-Designer project is the design-time environment for creating an OEE application.  The Designer is a GUI application for defining the plant equipment entities, data sources, event resolution scripts, manufacturing work schedule, availability reasons, produced materials and units of measure for the data collectors.  The Designer also includes a dashboard and trending capabilities.

## Overview
The Point85 Overall Equipment Effectiveness (OEE) applications enable:
* collection of equipment data from multiple sources to support OEE calculations , 
* resolution of a collected data value into an availability reason or produced material quantity to provide input to the performance, availability and quality components of OEE
* calculation of the OEE key performance indicator (KPI) for the equipment using an optional  work schedule for defining the scheduled production time
* monitoring of equipment availability, performance and quality events

Sources of equipment availability, performance and quality event data include:
* Manual: web browser-based data entry
* OPC DA:  classic OLE for Process Control (OPC) Data Acquisition (DA)
* OPC UA:  OLE for Process Control Unified Architecture (UA)
* HTTP: invocation of a web service via an HTTP request 
* Messaging:  an equipment event message received via a RabbitMQ message broker

The Point85 applications supporting OEE are:
* Designer:  a GUI application for defining the plant equipment, data sources, event resolution scripts, manufacturing work schedule, availability reasons, produced materials and units of measure for data collectors.  * The designer also includes a dashboard and trending capabilities.
* Collector:  a Windows service or Unix deamon to collect the equipment event data and store it in a relational database
* Monitor:  a GUI application with a dashboard to view the current equipment OEE and status
* Operator:  a web-application for manual entry of equipment events

In addition, two GUI test applications assist in the development of an OEE solution:
* HTTP requester and RabbitMQ message publisher
* Front end GUI for a data collector

## OEE Calculations
OEE is the product of equipment availability, performance and quality each expressed as a percentage.  The time-loss model is used to accumulate time in loss categories (or “no loss” if the equipment is running normally).  See [Kennedy] for details.  A data source provides an input value to a data collector’s resolver JavaScript function that maps that input value to an output value (reason or production count).

For availability and performance, the output value is a reason that is assigned to one of the following loss categories:
* Value Adding:  the “no loss” or “running OK” category.
* Not Scheduled: this is non-working time.  Non-working periods (e.g. holidays) typically are planned in the work schedule that is assigned to a plant entity.
* Unscheduled:  working time when the equipment is not scheduled for normal production (e.g. an R&D or laboratory test run).
* Planned Downtime:  working time when the equipment is not scheduled for normal production but the activity is intended to support production (e.g. planned preventive maintenance).
* Unplanned Downtime:  working time when the equipment is not available due to an unexpected fault (e.g. motor failure or jam).
* Setup:  working time when the equipment is being changed over in order to run new material.
* Stoppages: minor or short periods of time when the equipment is not producing as expected (such as a blocked or starved condition).
* Reduced Speed:  the equipment is producing, but not at its design speed or ideal run rate.
For quality or  yield, the data source provides a production count in the good, reject/rework or startup & yield categories in the defined units of measure for the material being produced.

## Architecture
The OEE applications can be grouped by design-time and run-time.  The design-time Designer application is used to define the plant equipment, data sources, event resolution scripts, manufacturing work schedule, availability reasons, produced materials and units of measure for data collectors.  The designer also includes a dashboard and trending capabilities.

An automated run-time data collector receives an input value from an OPC DA, OPC UA, HTTP or messaging source and executes a JavaScript resolver on this input to calculate an output value.  The output value is a reason (mapped to an OEE loss category) for an availability event, a new production count (good, reject/rework or startup) for performance and quality events or a material/job change event.  The event data is stored in a relational database where it is available for OEE calculations.  Both Microsoft SQL Server and Oracle are currently supported.

A web-based manual data collector records the OEE events based on information entered by an operator.  Similar to the automated collector, this data is also stored in the relational database.
If the system is configured for messaging, the event data is also sent to a RabbitMQ message broker to which a run-time monitor application can subscribe.  A monitor displays a dashboard for viewing equipment OEE events.  It also displays collector notifications and status information.

## Designer Application
The Designer is focused on configuring all aspects of equipment in order to enable OEE calculations.  It has editors for defining the plant model.  For example, the plant entity editor is:
![Plant Entity Editor](https://github.com/point85/OEE-Designer/blob/master/docs/designer-plant-entities.png)

The designer has a trending capability to observe the input and output values of a configured data source.  For example, an OPC DA variable trend is:
![OPC DA Trend](https://github.com/point85/OEE-Designer/blob/master/docs/designer-opc-da-trend.png)

For more information about the Designer and other OEE applications, please refer to the *Overall Equipment Effectiveness Applications User Guide* in the docs folder.
