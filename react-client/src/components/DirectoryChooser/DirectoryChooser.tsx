/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
import { ActionIcon, Box, Breadcrumbs, Button, Group, MantineSize, Modal, Paper, ScrollArea, Stack, TextInput, Tooltip } from '@mantine/core';
import axios from 'axios';
import { useState, ReactNode } from 'react';
import { IconCircleMinus, IconDevices2, IconFolder, IconFolders } from '@tabler/icons-react';

import { I18nInterface } from '../../services/i18n-service';
import { openGitHubNewIssue, settingsApiUrl } from '../../utils';
import { showError } from '../../utils/notifications';

export default function DirectoryChooser(props: {
  i18n: I18nInterface,
  tooltipText: string,
  path: string,
  callback: any,
  label?: string,
  disabled?: boolean,
  formKey?: string,
  size?: MantineSize,
  placeholder?: string,
  withAsterisk?: boolean
}) {
  const [isLoading, setLoading] = useState(true);
  const [opened, setOpened] = useState(false);
  const i18n = props.i18n;

  const [directories, setDirectories] = useState([] as { value: string, label: string }[]);
  const [parents, setParents] = useState([] as { value: string, label: string }[]);
  const [selectedDirectory, setSelectedDirectory] = useState('');
  const [separator, setSeparator] = useState('/');

  const selectAndCloseModal = (clear?: boolean) => {
    if (selectedDirectory || clear) {
      if (props.formKey) {
        props.callback(props.formKey, clear ? '' : selectedDirectory);
      } else {
        props.callback(clear ? '' : selectedDirectory);
      }
      return setOpened(false);
    }
    showError({
      title: i18n.get('Error'),
      message: i18n.get('NoDirectorySelected'),
    });
  };

  const getSubdirectories = (path: string) => {
    axios.post(settingsApiUrl + 'directories', { path: (path) ? path : '' })
      .then(function(response: any) {
        const directoriesResponse = response.data;
        setSeparator(directoriesResponse.separator);
        setDirectories(directoriesResponse.children);
        setParents(directoriesResponse.parents.reverse());
      })
      .catch(function() {
        showError({
          id: 'data-loading',
          title: i18n.get('Error'),
          message: i18n.get('SubdirectoriesNotReceived'),
          onClick: () => { openGitHubNewIssue(); },
        });
      })
      .then(function() {
        setLoading(false);
      });
  };

  const input = (): ReactNode => {
    return <TextInput
      size={props.size}
      label={props.label}
      disabled={props.disabled}
      style={{ flex: 1 }}
      value={props.path}
      placeholder={props.placeholder}
      withAsterisk={props.withAsterisk}
      readOnly
    />
  }

  return (
    <Group>
      <>
        <Modal
          opened={opened}
          onClose={() => setOpened(false)}
          title={
            <Group>
              <IconFolders />
              {i18n.get('SelectedDirectory')}
            </Group>
          }
          scrollAreaComponent={ScrollArea.Autosize}
          size='lg'
        >
          <Box mx='auto'>
            <Paper shadow='md' p='xs' withBorder>
              <Group>
                <Breadcrumbs separator={separator}>
                  <Button
                    loading={isLoading}
                    onClick={() => getSubdirectories('roots')}
                    variant='default'
                    size='compact-md'
                  >
                    <IconDevices2 />
                  </Button>
                  {parents.map(parent => (
                    <Button
                      loading={isLoading}
                      onClick={() => getSubdirectories(parent.value)}
                      key={'breadcrumb' + parent.label}
                      variant='default'
                      size='compact-md'
                    >
                      {parent.label}
                    </Button>
                  ))}
                </Breadcrumbs>
              </Group>
            </Paper>
            <Stack gap='xs' align='flex-start' justify='flex-start' mt='sm'>
              {directories.map(directory => (
                <Group key={'group' + directory.label}>
                  <Button
                    leftSection={<IconFolder size={18} />}
                    variant={(selectedDirectory === directory.value) ? 'light' : 'subtle'}
                    loading={isLoading}
                    onClick={() => setSelectedDirectory(directory.value)}
                    onDoubleClick={() => getSubdirectories(directory.value)}
                    key={directory.label}
                    size='compact-md'
                  >
                    {directory.label}
                  </Button>
                  {selectedDirectory === directory.value &&
                    <Button
                      variant='filled'
                      loading={isLoading}
                      onClick={() => selectAndCloseModal()}
                      key={'select' + directory.label}
                      size='compact-md'
                    >
                      Select
                    </Button>
                  }
                </Group>
              ))}
            </Stack>
          </Box>
        </Modal>

        {props.tooltipText ? (<Tooltip label={props.tooltipText} style={{ width: 350 }} color={'blue'} multiline withArrow={true}>
          {input()}
        </Tooltip>) : input()
        }
        {!props.disabled && (
          <>
            <Button
              mt={props.label ? '24px' : undefined}
              size={props.size}
              onClick={() => { getSubdirectories(props.path); setOpened(true); }}
              leftSection={<IconFolders size={18} />}
            >
              ...
            </Button>
            <ActionIcon
              mt={props.label ? '24px' : undefined}
              size={props.size}
              onClick={() => selectAndCloseModal(true)}
              variant='default'
            >
              <IconCircleMinus size={18} />
            </ActionIcon>
          </>
        )}
      </>
    </Group>
  );
}

DirectoryChooser.defaultProps = {
  tooltipText: null,
}
