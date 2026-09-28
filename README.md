Multiplex analysis in QuPath frequently involves classifying cells as positive or negative for >10 markers, leading to potentially hundreds or thousands of unique classes. To get the total number of cells of any class X, you must add the counts of any classification that contains X. This extension creates a simple UI and workflow command to make that easy. 

**How To Use**

<img width="405" height="554" alt="image" src="https://github.com/user-attachments/assets/4816efaa-3c70-4189-a981-4b8f37a0caa4" />

(Example interface)

This will generate a check list of all the base classes that exist in your image. Check one, click `Calculate Sum`. For each annotation, it will add the counts of all cells that contain that class in their classification list. If you check more than one (for example, `Tcell` and `CD4`) it will sum all counts that contain both of them in any other (`Tcell: CD4...`). It will *not* get confused by string similiarity, so that it doesn't add all `CD45` when searching for `CD4`. 

Alternatively, you can click `Calculate All Combos`. If no classes are selected, it calculates the total number of each base class, individually, for each annotation. If any class(es) is selected, it finds cells that are the selected class with all base classes. 

**Batch processing**
It writes each calculation as a workflow step, so that you can easily generate a script and run it for a whole project. 

**To install**

Drag the .jar into QuPath. 
