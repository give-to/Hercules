import _Applier as app
import sys
import os
import time


'''
project = sys.argv[1]
'''
backup_bug_path = sys.argv[1]


# Read All bugs in CURR path
curr = os.getcwd()
contents = os.listdir(os.path.join(curr,'patches'))
bug_lists = [bug_list for bug_list in contents if os.path.isdir(os.path.join(os.path.join(curr,'patches'), bug_list))]

if "__pycache__" in bug_lists:
    bug_lists.remove("__pycache__")

os.system('echo -n \'\' > %s/time_info.txt' % curr)

for bug_list in bug_lists:
    os.chdir(os.path.join(curr,'patches', bug_list))
    
    #Control work flow
    check_next_bug = False
    last_patch = False

    #get start time
    start_time = time.time()
    with open('time_info.txt', 'r') as time_info:
        time_info_read = time_info.read()
        time_info_read_new = time_info_read.replace('\n','')
        used_time = float(time_info_read_new[:-1])
        earn_time = 18000.00 - used_time
        if earn_time < 0:
            print("Set timeout error, now timeout is %f" % earn_time)
            sys.exit()

    bug_curr = os.getcwd()
    if not os.path.exists('%s/results' % curr):
        os.system('mkdir %s/results' % curr)
    os.system('echo -n \'\' > %s/results/%s.txt' % (curr, bug_list))
    
    #Get patches
    patches_dir = []
    with open('%s/rank.txt' % bug_curr, 'r') as patch_ranks:
        for patch_rank in patch_ranks:
            if patch_rank == '\n':
                continue
            patches_dir.append(patch_rank.strip())

    # Create and clear tmp
    if not os.path.exists('%s/tmp' % curr):
        os.system('mkdir %s/tmp' % curr)

    # cp bug project from bakcup to /tmp
    if not os.path.exists('%s/tmp/%s' % (curr,bug_list)):
        os.system('cp -r %s/%s %s/tmp/%s' % (backup_bug_path, bug_list, curr, bug_list))
    else:
        os.system('rm -rf %s/tmp/%s' % (curr,bug_list))
        os.system('cp -r %s/%s %s/tmp/%s' % (backup_bug_path, bug_list, curr, bug_list))

    #Iter patches
    for patch_dir in patches_dir:
        try:
            abs_patch_dir = os.path.join(bug_curr, patch_dir)

            # Check if the last patch
            if patch_dir == patches_dir[-1]:
                last_patch = True

            # Apply patch
            with open(abs_patch_dir, 'r') as p:
                rel_file_pathes = []
                lineids = []
                patches = []
                file_pathes = []
                ori_files = []

                for line in p:
                    if line == '\n':
                        continue

                    rel_file_path, lineid, patch = line.split(':', 2)
                    file_path = os.path.join(curr, 'tmp', bug_list, rel_file_path)
                    ori_file = os.path.basename(file_path)

                    rel_file_pathes.append(rel_file_path)
                    lineids.append(lineid)
                    patches.append(patch)
                    file_pathes.append(file_path)
                    ori_files.append(ori_file)
                    '''
                    if line == '\n':
                        continue
                    # file_path is absolute path here
                    rel_file_path,lineid,patch =line.split(':',2)
                    file_path = os.path.join(curr,'tmp',bug_list,rel_file_path)
                    ori_file = os.path.basename(file_path)

                    with open(file_path,'r', encoding="latin-1") as f:
                        src = f.readlines() 
                        src_app = app.applyPatch(src, patch, int(lineid))
                    '''
                used_files = []
                for i in range(0, len(file_pathes)):
                    if file_pathes[i] not in used_files:
                        used_files.append(file_pathes[i])

                for used_file in used_files:
                    with open(used_file, 'r', encoding="latin-1") as f:
                        src = f.readlines()
                        src_app_old = src

                        for i in range(0, len(file_pathes)):
                            if used_file == file_pathes[i]:
                                src_app_new = app.applyPatch(src_app_old, patches[i], int(lineids[i]))
                                src_app_old = src_app_new

                    # Write Applied src to file
                    out_app = app.toSrc(src_app_new)
                    with open(used_file, 'w', encoding="latin-1") as des_file:
                        des_file.write(out_app)

                dir_classes = os.popen('catena4j export -p dir.bin.classes -w %s/tmp/%s' % (curr, bug_list)).readlines()
                os.system('rm -rf %s/tmp/%s/%s' % (curr, bug_list, dir_classes[0]))
                failing_test = os.popen('timeout -s 9 300 catena4j test -w %s/tmp/%s' % (curr, bug_list)).readlines()
                if not failing_test:
                    with open('%s/results/%s.txt' % (curr, bug_list), 'at') as out:
                        out.write('%s:Build Error\n' % patch_dir)
                        out.close()
                elif failing_test[0].strip() == "Failing tests: 0":
                    with open('%s/results/%s.txt' % (curr, bug_list), 'at') as out:
                        out.write('%s:Pass\n' % patch_dir)
                        out.close()
                else:
                    with open('%s/results/%s.txt' % (curr, bug_list), 'at') as out:
                        out.write('%s:Fail\n' % patch_dir)
                        out.close()

                for used_file in used_files:
                    back, relative = used_file.split(bug_list, 1)
                    os.system('cp %s/%s/%s %s' % (backup_bug_path, bug_list, relative, used_file))

        except Exception as e:
            with open('%s/results/%s.txt' % (curr, bug_list), 'at') as out:
                out.write('%s:Validation Throw Exception!\n' % patch_dir)
                out.close()

            os.system('rm -rf %s/tmp/%s' % (curr, bug_list))
            os.system('cp -r %s/%s %s/tmp/%s' % (backup_bug_path, bug_list, curr, bug_list))

        #Check time after validated a patch
        tmp_time = time.time()
        time_difference = tmp_time - start_time
            
        if last_patch == True:
            print("Last patch#################################3")
            check_next_bug = True

        if time_difference >= earn_time:
            print("Time out#################################3")
            check_next_bug = True
                
        if check_next_bug == True:
            time_difference_min = round(float(time_difference / 60.00),2)
            total_time_min = round(float((time_difference + used_time) / 60.00),2)
            used_time_min = round(float(used_time / 60.00),2)
            with open('%s/time_info.txt' % curr, 'at') as time_out:
                time_out.write('%s:\n' % bug_list)
                time_out.write('Generation Time: %smin\n' % str(used_time_min))
                time_out.write('Validation Time: %smin\n' % str(time_difference_min))
                time_out.write('Total Time: %smin\n' % str(total_time_min))
            break

