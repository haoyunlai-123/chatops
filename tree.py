import os
import sys

def generate_tree(startpath, output_file, prefix='', ignore=None):
    """
    递归生成目录树并写入文件
    :param startpath: 起始目录路径
    :param output_file: 打开的文件对象
    :param prefix: 当前层级的前缀字符串（用于缩进）
    :param ignore: 需要忽略的文件/目录名列表
    """
    if ignore is None:
        ignore = []
    try:
        items = sorted(os.listdir(startpath))
    except PermissionError:
        output_file.write(prefix + "[权限不足，无法访问]\n")
        return

    # 过滤掉需要忽略的项目
    items = [item for item in items if item not in ignore]

    for i, item in enumerate(items):
        path = os.path.join(startpath, item)
        is_last = (i == len(items) - 1)
        # 根据是否是最后一个项目选择线条符号
        current_prefix = '└── ' if is_last else '├── '
        output_file.write(prefix + current_prefix + item)

        if os.path.isdir(path):
            output_file.write('/\n')
            # 子目录的缩进：如果当前是最后一个，则后续用空格，否则用竖线
            extension = '    ' if is_last else '│   '
            generate_tree(path, output_file, prefix + extension, ignore)
        else:
            output_file.write('\n')

if __name__ == '__main__':
    output_filename = '项目结构.txt'
    start_dir = os.getcwd()  # 当前工作目录
    script_name = os.path.basename(__file__)

    # 需要忽略的文件夹列表
    ignore_dirs = ['.mvn', '.idea', '.git','target']
    ignore_list = [output_filename, script_name] + ignore_dirs

    with open(output_filename, 'w', encoding='utf-8') as f:
        f.write(start_dir + '\n')
        generate_tree(start_dir, f, ignore=ignore_list)

    print(f'目录树已生成到 {output_filename}')